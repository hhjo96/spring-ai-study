package com.study.bookadvisor.service;

import com.study.bookadvisor.dto.BookSearchResult;
import com.study.bookadvisor.tool.BookEventTools;
import com.study.bookadvisor.tool.BookInventoryTools;
import com.study.bookadvisor.tool.BookReviewSearchTools;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SafeGuardAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI 도서 추천 에이전트 서비스
 *
 * Tool Calling + RAG + Memory를 결합한 AI 도서 추천 에이전트입니다.
 *
 * [아키텍처]
 * 사용자 질문
 *   → SafeGuardAdvisor (민감 단어 차단)
 *   → PromptChatMemoryAdvisor (이전 대화 기억 추가)
 *   → VectorStore 시맨틱 검색 (관련 도서 컨텍스트)
 *   → LLM + Tool Calling (서평 검색 + 재고/가격 조회 + 이벤트 조회)
 *   → SimpleLoggerAdvisor (요청/응답 로깅)
 *   → 사용자에게 응답 반환
 *
 * [사용 도구 목록]
 * 1. BookReviewSearchTools.searchBookReview() — 인터넷에서 전문가 서평/리뷰 링크 검색
 * 2. BookReviewSearchTools.fetchPageContent()  — 서평 웹 페이지 본문 텍스트 추출
 * 3. BookInventoryTools.checkInventoryByTitle() — 도서 제목으로 재고/가격 조회
 * 4. BookInventoryTools.checkInventoryByIsbn()  — ISBN으로 재고/가격 조회
 * 5. BookEventTools.checkEventByTitle()         — 도서 제목으로 진행 중인 이벤트 조회
 */
@Slf4j
@Service
public class BookAgentService {

    private final ChatClient chatClient;
    private final BookSearchService bookSearchService;

    private final BookReviewSearchTools bookReviewSearchTools;
    private final BookInventoryTools bookInventoryTools;
    private final BookEventTools bookEventTools;

    // ========================================================================
    // TODO 6: ChatClient 빌드 — Advisor + 시스템 프롬프트 구성
    // ========================================================================
    /**
     * [요구사항]
     * ChatClient를 빌드할 때 아래 설정을 적용하세요.
     *
     * 1) 시스템 프롬프트 (아래 내용 그대로 사용):
     *    """
     *    당신은 전문 도서 추천 에이전트입니다.
     *    사용자의 관심사, 독서 수준, 선호 장르를 파악하여 적절한 도서를 추천해주세요.
     *
     *    추천할 때는 반드시 다음 정보를 포함해주세요:
     *    1. 책 제목, 저자, 장르
     *    2. 추천 이유 (왜 이 사용자에게 적합한지)
     *    3. 전문가 서평 근거 및 링크 (searchBookReview 도구로 검색 → <a href> 태그로 링크 제공)
     *    4. 현재 가격과 재고 상태 (checkInventoryByTitle 도구 활용)
     *    5. 진행 중인 이벤트/프로모션 (checkEventByTitle 도구 활용)
     *
     *    서평 검색 결과에서 링크가 있으면 반드시 <a href="URL" target="_blank">제목</a> 형태로 제공하세요.
     *    이전 대화 내용을 참고하여 일관성 있는 추천을 해주세요.
     *    답변은 HTML 형식으로 작성해주세요. <div> 안에 들어가는 내용으로만 답변하세요.
     *    <h1> 태그는 사용하지 마세요. <h4>, <p>, <ul>, <li>, <strong>, <em>, <span>, <a> 태그를 사용하세요.
     *    가격은 천 단위 콤마를 포함해서 표기해주세요.
     *    """
     *
     * 2) Advisor 체인 (defaultAdvisors):
     *    a) SafeGuardAdvisor
     *       - 차단 단어: "욕설", "폭력", "도박", "불법 다운로드"
     *       - 차단 메시지: "해당 질문은 도서 추천 서비스에서 다룰 수 없는 내용입니다."
     *       - 우선순위: Ordered.HIGHEST_PRECEDENCE
     *    b) PromptChatMemoryAdvisor
     *       - JdbcChatMemoryRepository 기반 MessageWindowChatMemory (maxMessages: 50)
     *    c) SimpleLoggerAdvisor
     *       - 우선순위: Ordered.LOWEST_PRECEDENCE - 1
     *
     * [힌트]
     * - ChatMemory 생성:
     *   ChatMemory chatMemory = MessageWindowChatMemory.builder()
     *       .chatMemoryRepository(chatMemoryRepository)
     *       .maxMessages(50)
     *       .build();
     *
     * - ChatClient 빌드:
     *   ChatClient.builder(chatModel)
     *       .defaultSystem("시스템 프롬프트")
     *       .defaultAdvisors(safeguard, memory, logger)
     *       .build();
     */
    public BookAgentService(
            ChatModel chatModel,
            JdbcChatMemoryRepository chatMemoryRepository,
            BookSearchService bookSearchService,
            BookReviewSearchTools bookReviewSearchTools,
            BookInventoryTools bookInventoryTools,
            BookEventTools bookEventTools) {

        this.bookSearchService = bookSearchService;

        // ChatMemory 생성
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(50)
                .build();

        // Advisor 생성
        SafeGuardAdvisor safeGuardAdvisor = new SafeGuardAdvisor(
                List.of("욕설", "폭력", "도박", "불법 다운로드"),
                "해당 질문은 도서 추천 서비스에서 다룰 수 없는 내용입니다.",
                Ordered.HIGHEST_PRECEDENCE
        );

        PromptChatMemoryAdvisor memoryAdvisor = PromptChatMemoryAdvisor.builder(chatMemory)
                .build();

        SimpleLoggerAdvisor loggerAdvisor = new SimpleLoggerAdvisor(Ordered.LOWEST_PRECEDENCE - 1);

        // ChatClient 빌드
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem("""
                        당신은 전문 도서 추천 에이전트입니다.
                        사용자의 관심사, 독서 수준, 선호 장르를 파악하여 적절한 도서를 추천해주세요.

                        추천할 때는 반드시 다음 정보를 포함해주세요:
                        1. 책 제목, 저자, 장르
                        2. 추천 이유 (왜 이 사용자에게 적합한지)
                        3. 전문가 서평 근거 및 링크 (searchBookReview 도구로 검색 → <a href> 태그로 링크 제공)
                        4. 현재 가격과 재고 상태 (checkInventoryByTitle 도구 활용)
                        5. 진행 중인 이벤트/프로모션 (checkEventByTitle 도구 활용)

                        서평 검색 결과에서 링크가 있으면 반드시 <a href="URL" target="_blank">제목</a> 형태로 제공하세요.
                        이전 대화 내용을 참고하여 일관성 있는 추천을 해주세요.
                        답변은 HTML 형식으로 작성해주세요. <div> 안에 들어가는 내용으로만 답변하세요.
                        <h1> 태그는 사용하지 마세요. <h4>, <p>, <ul>, <li>, <strong>, <em>, <span>, <a> 태그를 사용하세요.
                        가격은 천 단위 콤마를 포함해서 표기해주세요.
                        """)
                .defaultAdvisors(safeGuardAdvisor, memoryAdvisor, loggerAdvisor)
                .build();
        this.bookReviewSearchTools = bookReviewSearchTools;
        this.bookInventoryTools = bookInventoryTools;
        this.bookEventTools = bookEventTools;
    }

    // ========================================================================
    // TODO 7: 도구 호출을 활용한 AI 도서 추천 에이전트 채팅
    // ========================================================================
    /**
     * VectorStore 검색 + Tool Calling + Memory를 결합한 에이전트 채팅입니다.
     *
     * [요구사항]
     * 1) bookSearchService.searchSimple()로 사용자 질문과 유사한 도서를 VectorStore에서 검색
     * 2) 검색된 도서 정보를 컨텍스트 문자열로 조합
     * 3) chatClient를 호출할 때:
     *    a) .user()에 사용자 메시지 + VectorStore 검색 컨텍스트를 전달
     *    b) .tools()에 3개 도구 클래스를 전달 ← 핵심!
     *       - bookReviewSearchTools (서평 검색)
     *       - bookInventoryTools (재고/가격 조회)
     *       - bookEventTools (이벤트 조회)
     *    c) .advisors()에서 ChatMemory.CONVERSATION_ID에 conversationId를 전달
     * 4) LLM 응답을 반환
     *
     * [힌트]
     * 1) VectorStore 검색:
     *    List<BookSearchResult> results = bookSearchService.searchSimple(userMessage);
     *
     * 2) 컨텍스트 생성:
     *    StringBuilder context = new StringBuilder("\n[보유 도서 목록 (VectorStore 검색 결과)]\n");
     *    results.forEach(r -> context.append("- ").append(r.content()).append("\n"));
     *
     * 3) ChatClient 호출 (tools() 메소드가 핵심):
     *    chatClient.prompt()
     *        .user(userMessage + context.toString())
     *        .tools(bookReviewSearchTools, bookInventoryTools, bookEventTools)
     *        .advisors(advisorSpec -> advisorSpec.param(
     *            ChatMemory.CONVERSATION_ID, conversationId))
     *        .call()
     *        .content();
     *
     * @param userMessage    사용자 메시지
     * @param conversationId 대화 식별자
     * @return LLM의 도서 추천 응답 (서평 링크 + 재고/가격 + 이벤트 포함)
     */
    public String chat(String userMessage, String conversationId) {
        log.info("AI 도서 추천 에이전트 채팅: conversationId={}, message={}", conversationId, userMessage);

        // 1) VectorStore 검색
        List<BookSearchResult> results = bookSearchService.searchSimple(userMessage);

        // 2) 컨텍스트 생성
        StringBuilder context = new StringBuilder("\n[보유 도서 목록 (VectorStore 검색 결과)]\n");
        results.forEach(r -> context.append("- ").append(r.content()).append("\n"));

        // 3) ChatClient 호출 (tools() 메소드로 도구 전달)
        String response = chatClient.prompt()
                .user(userMessage + context.toString())
                .tools(bookReviewSearchTools, bookInventoryTools, bookEventTools)
                .advisors(advisorSpec -> advisorSpec.param(
                        ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();

        log.info("AI 응답 생성 완료: conversationId={}", conversationId);
        return response;
    }
}




