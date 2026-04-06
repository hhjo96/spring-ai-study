package com.study.springai.dto;

public record RagChatRequest(
        String question,
        double score,
        String source,
        String conversationId,
        String ragType  // "basic", "compression", "rewrite", "translation", "multiQuery"
) {
    public RagChatRequest {
        if (score <= 0) score = 0.5;
        if (ragType == null || ragType.isBlank()) ragType = "basic";
        if (conversationId == null || conversationId.isBlank()) conversationId = "default";
    }
}
