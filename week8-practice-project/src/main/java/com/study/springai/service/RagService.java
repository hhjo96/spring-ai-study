package com.study.springai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.CompressionQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.TranslationQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class RagService {

    // ##### 필드 #####
    private final ChatClient chatClient;
    private final ChatModel chatModel;
    private final VectorStore vectorStore;

    private final ChatMemory chatMemory = MessageWindowChatMemory.builder()
            .chatMemoryRepository(new InMemoryChatMemoryRepository())
            .build();

    // ##### 생성자 #####
    public RagService(ChatClient.Builder chatClientBuilder, ChatModel chatModel, VectorStore vectorStore) {
        this.chatClient = chatClientBuilder
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(Ordered.LOWEST_PRECEDENCE - 1)
                )
                .build();
        this.chatModel = chatModel;
        this.vectorStore = vectorStore;
    }

    // ===== QuestionAnswerAdvisor 방식 (기본 RAG) =====
    public String ragChat(String question, double score, String source) {
        SearchRequest.Builder searchRequestBuilder = SearchRequest.builder()
                .similarityThreshold(score)
                .topK(3);
        if (StringUtils.hasText(source)) {
            searchRequestBuilder.filterExpression("source == '%s'".formatted(source));
        }
        SearchRequest searchRequest = searchRequestBuilder.build();

        QuestionAnswerAdvisor questionAnswerAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(searchRequest)
                .build();

        return this.chatClient.prompt()
                .user(question)
                .advisors(questionAnswerAdvisor)
                .call()
                .content();
    }

    // ===== CompressionQueryTransformer 방식 =====
    public String chatWithCompression(String question, double score, String source, String conversationId) {
        RetrievalAugmentationAdvisor retrievalAugmentationAdvisor =
                RetrievalAugmentationAdvisor.builder()
                        .queryTransformers(createCompressionQueryTransformer())
                        .documentRetriever(createVectorStoreDocumentRetriever(score, source))
                        .build();

        String answer = this.chatClient.prompt()
                .user(question)
                .advisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        retrievalAugmentationAdvisor
                )
                .advisors(advisorSpec -> advisorSpec.param(
                        ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();
        return answer;
    }

    // ===== RewriteQueryTransformer 방식 =====
    public String chatWithRewriteQuery(String question, double score, String source) {
        RetrievalAugmentationAdvisor retrievalAugmentationAdvisor =
                RetrievalAugmentationAdvisor.builder()
                        .queryTransformers(createRewriteQueryTransformer())
                        .documentRetriever(createVectorStoreDocumentRetriever(score, source))
                        .build();

        String answer = this.chatClient.prompt()
                .user(question)
                .advisors(retrievalAugmentationAdvisor)
                .call()
                .content();
        return answer;
    }

    // ===== TranslationQueryTransformer 방식 =====
    public String chatWithTranslation(String question, double score, String source) {
        RetrievalAugmentationAdvisor retrievalAugmentationAdvisor =
                RetrievalAugmentationAdvisor.builder()
                        .queryTransformers(createTranslationQueryTransformer())
                        .documentRetriever(createVectorStoreDocumentRetriever(score, source))
                        .build();

        String answer = this.chatClient.prompt()
                .user(question)
                .advisors(retrievalAugmentationAdvisor)
                .call()
                .content();
        return answer;
    }

    // ===== MultiQueryExpander 방식 =====
    public String chatWithMultiQuery(String question, double score, String source) {
        RetrievalAugmentationAdvisor retrievalAugmentationAdvisor =
                RetrievalAugmentationAdvisor.builder()
                        .queryExpander(createMultiQueryExpander())
                        .documentRetriever(createVectorStoreDocumentRetriever(score, source))
                        .build();

        String answer = this.chatClient.prompt()
                .user(question)
                .advisors(retrievalAugmentationAdvisor)
                .call()
                .content();
        return answer;
    }

    // ===== 모듈 생성 헬퍼 메소드들 =====

    private CompressionQueryTransformer createCompressionQueryTransformer() {
        ChatClient.Builder chatClientBuilder = ChatClient.builder(chatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor(Ordered.LOWEST_PRECEDENCE - 1));

        return CompressionQueryTransformer.builder()
                .chatClientBuilder(chatClientBuilder)
                .build();
    }

    private RewriteQueryTransformer createRewriteQueryTransformer() {
        ChatClient.Builder chatClientBuilder = ChatClient.builder(chatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor(Ordered.LOWEST_PRECEDENCE - 1));

        return RewriteQueryTransformer.builder()
                .chatClientBuilder(chatClientBuilder)
                .build();
    }

    private TranslationQueryTransformer createTranslationQueryTransformer() {
        ChatClient.Builder chatClientBuilder = ChatClient.builder(chatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor(Ordered.LOWEST_PRECEDENCE - 1));

        return TranslationQueryTransformer.builder()
                .chatClientBuilder(chatClientBuilder)
                .targetLanguage("Korean")
                .build();
    }

    private MultiQueryExpander createMultiQueryExpander() {
        ChatClient.Builder chatClientBuilder = ChatClient.builder(chatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor(Ordered.LOWEST_PRECEDENCE - 1));

        return MultiQueryExpander.builder()
                .chatClientBuilder(chatClientBuilder)
                .includeOriginal(true)
                .numberOfQueries(3)
                .build();
    }

    private VectorStoreDocumentRetriever createVectorStoreDocumentRetriever(double score, String source) {
        return VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .similarityThreshold(score)
                .topK(3)
                .filterExpression(() -> {
                    FilterExpressionBuilder builder = new FilterExpressionBuilder();
                    if (StringUtils.hasText(source)) {
                        return builder.eq("source", source).build();
                    } else {
                        return null;
                    }
                })
                .build();
    }
}
