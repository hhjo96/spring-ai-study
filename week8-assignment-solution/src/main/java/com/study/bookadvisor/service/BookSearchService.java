package com.study.bookadvisor.service;

import com.study.bookadvisor.dto.BookSearchResult;
import com.study.bookadvisor.dto.EmbedResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 시맨틱 도서 검색 서비스
 *
 * VectorStore를 활용하여 자연어 질의로 유사한 도서를 검색합니다.
 * 이 서비스는 week6에서 학습한 RAG 기반 검색을 그대로 활용합니다.
 *
 * week8에서는 이 검색 결과를 BookAgentService에서 도구 호출과 함께 사용하여
 * 더 풍부한 도서 추천을 제공합니다.
 */
@Slf4j
@Service
public class BookSearchService {

    private final VectorStore vectorStore;
    private final EmbeddingModel embeddingModel;

    public BookSearchService(VectorStore vectorStore, EmbeddingModel embeddingModel) {
        this.vectorStore = vectorStore;
        this.embeddingModel = embeddingModel;
    }

    /**
     * 자연어 쿼리로 유사한 도서를 검색합니다. (week6 복습 — 제공됨)
     */
    public List<BookSearchResult> searchSimple(String query) {
        List<Document> docs = vectorStore.similaritySearch(query);
        return docs.stream().map(this::toSearchResult).toList();
    }

    /**
     * topK, similarityThreshold, 장르 필터를 적용한 상세 유사도 검색 (week6 복습 — 제공됨)
     */
    public List<BookSearchResult> searchAdvanced(String query, int topK,
                                                  double similarityThreshold, String genre) {
        SearchRequest.Builder builder = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(similarityThreshold);

        if (genre != null && !genre.isBlank()) {
            builder.filterExpression("genre == '" + genre + "'");
        }

        List<Document> docs = vectorStore.similaritySearch(builder.build());
        return docs.stream().map(this::toSearchResult).toList();
    }

    /**
     * 텍스트를 임베딩하고 벡터 정보를 반환합니다. (week6 복습 — 제공됨)
     */
    public EmbedResponse getEmbeddingInfo(String text) {
        EmbeddingResponse response = embeddingModel.embedForResponse(List.of(text));
        String model = response.getMetadata().getModel();
        int dimensions = embeddingModel.dimensions();
        float[] vector = response.getResults().get(0).getOutput();
        int vectorLength = vector.length;
        List<Float> vectorSample = new ArrayList<>();
        for (int i = 0; i < Math.min(5, vectorLength); i++) {
            vectorSample.add(vector[i]);
        }
        return new EmbedResponse(model, dimensions, vectorLength, vectorSample);
    }

    /**
     * Document를 BookSearchResult로 변환하는 헬퍼 메소드
     */
    private BookSearchResult toSearchResult(Document doc) {
        return new BookSearchResult(
                doc.getId(),
                doc.getText(),
                doc.getMetadata(),
                doc.getScore()
        );
    }
}
