package com.study.bookadvisor.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 도서 서평/전문가 리뷰 검색 도구
 *
 * Naver 검색 API를 사용하여 인터넷에서 도서의 전문가 서평, 리뷰 기사,
 * 북 리뷰 블로그 등의 링크를 검색합니다.
 *
 * LLM이 도서를 추천할 때, 전문가의 평가를 근거로 제시하고
 * 사용자가 직접 서평을 확인할 수 있도록 링크를 함께 제공합니다.
 *
 * [아키텍처]
 * 사용자 질문 → LLM 판단 → 서평 검색 도구 호출
 *   → Naver 검색 API 호출 → 서평 링크 목록 반환
 *   → (선택) 웹 페이지 본문 추출 → LLM에게 서평 내용 전달
 *   → LLM이 서평 근거 + 링크를 포함한 HTML 응답 생성
 *
 * [Chapter 11 참고]
 * - @Tool: 메소드를 도구로 정의, description으로 LLM이 호출 시점 판단
 * - @ToolParam: 매개변수 설명, LLM이 적절한 매개값을 생성하도록 안내
 * - RestClient: HTTP 클라이언트로 외부 API 호출
 */
@Component
@Slf4j
public class BookReviewSearchTools {

    // ##### 필드 #####
    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ##### 생성자 #####
    public BookReviewSearchTools(
            @Value("${naver.search.blog-endpoint}") String blogEndpoint,
            @Value("${naver.search.client-id}") String clientId,
            @Value("${naver.search.client-secret}") String clientSecret
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(blogEndpoint)
                .defaultHeader("X-Naver-Client-Id", clientId)
                .defaultHeader("X-Naver-Client-Secret", clientSecret)
                .defaultHeader("Accept", "application/json")
                .build();
    }

    // ========================================================================
    // TODO 1: 도서 서평/전문가 리뷰 인터넷 검색 도구
    // ========================================================================
    /**
     * 도서 제목으로 인터넷에서 전문가 서평과 리뷰 링크를 검색합니다.
     *
     * Naver 블로그 검색 API를 호출하여 해당 도서에 대한 전문가 서평, 리뷰 기사,
     * 북 리뷰 블로그 등의 링크를 찾습니다. LLM은 이 결과를 활용하여
     * 사용자에게 서평 근거와 함께 클릭 가능한 링크(<a> 태그)를 제공합니다.
     *
     * @param bookTitle 검색할 도서 제목
     * @return 서평 검색 결과 문자열 (제목, 링크URL, 요약)
     */
    @Tool(description = "도서 제목으로 인터넷에서 전문가 서평과 리뷰 링크를 검색합니다. 제목, 링크URL, 요약을 반환하며 LLM은 이를 클릭 가능한 HTML 링크로 변환하여 사용자에게 제공합니다.")
    public String searchBookReview(
            @ToolParam(description = "검색할 도서 제목", required = true) String bookTitle) {
        log.info("도서 리뷰 검색 도구 호출: bookTitle={}", bookTitle);
        try {
            String responseBody = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("query", bookTitle + " 서평 리뷰")
                            .queryParam("display", 3)
                            .queryParam("sort", "sim")
                            .build())
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode items = root.path("items");

            if (items.isMissingNode() || items.isEmpty()) {
                return "검색 결과가 없습니다.";
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(3, items.size()); i++) {
                JsonNode item = items.get(i);
                // Naver 검색 결과에 포함된 HTML 태그(<b> 등) 제거
                String title = item.path("title").asText().replaceAll("<.*?>", "");
                String description = item.path("description").asText().replaceAll("<.*?>", "");
                sb.append("[").append(i + 1).append("] ")
                        .append(title).append("\n")
                        .append(item.path("link").asText()).append("\n")
                        .append(description).append("\n\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("인터넷 검색 중 오류 발생", e);
            return "인터넷 검색 중 오류 발생: " + e.getMessage();
        }
    }

    // ========================================================================
    // TODO 2: 웹 페이지 본문 텍스트 추출 도구
    // ========================================================================
    /**
     * 서평 웹 페이지 URL에서 본문 텍스트를 추출합니다.
     *
     * searchBookReview()로 찾은 서평 링크의 실제 내용을 가져올 때 사용합니다.
     * LLM이 서평의 상세 내용을 파악하여 더 풍부한 추천 근거를 제시할 수 있도록 합니다.
     *
     * @param url 본문을 추출할 서평 웹 페이지 URL
     * @return 웹 페이지 본문 텍스트 (최대 2000자)
     */
    @Tool(description = "서평 웹 페이지의 본문 텍스트를 추출합니다. 검색된 서평 링크에서 상세 리뷰 내용을 가져올 때 사용합니다.")
    public String fetchPageContent(
            @ToolParam(description = "본문을 추출할 서평 웹 페이지 URL", required = true) String url) {
        log.info("웹 페이지 본문 추출 도구 호출: url={}", url);
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(5000)
                    .get();
            String bodyText = doc.body().text();

            if (bodyText == null || bodyText.isBlank()) {
                return "페이지 내용을 가져올 수 없습니다.";
            }

            if (bodyText.length() > 2000) {
                bodyText = bodyText.substring(0, 2000) + "...";
            }
            return bodyText;
        } catch (Exception e) {
            log.error("페이지 로딩 중 오류 발생", e);
            return "페이지 로딩 중 오류 발생: " + e.getMessage();
        }
    }
}
