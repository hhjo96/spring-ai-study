package com.study.springai.controller;

import com.study.springai.dto.ChatResponse;
import com.study.springai.dto.RagChatRequest;
import com.study.springai.service.RagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagService ragService;

    /**
     * RAG 채팅 - ragType에 따라 다른 전략 사용
     * ragType: "basic", "compression", "rewrite", "translation", "multiQuery"
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> ragChat(@RequestBody RagChatRequest request) {
        String answer = switch (request.ragType()) {
            case "compression" -> ragService.chatWithCompression(
                    request.question(), request.score(), request.source(), request.conversationId());
            case "rewrite" -> ragService.chatWithRewriteQuery(
                    request.question(), request.score(), request.source());
            case "translation" -> ragService.chatWithTranslation(
                    request.question(), request.score(), request.source());
            case "multiQuery" -> ragService.chatWithMultiQuery(
                    request.question(), request.score(), request.source());
            default -> ragService.ragChat(
                    request.question(), request.score(), request.source());
        };
        return ResponseEntity.ok(new ChatResponse(answer));
    }
}
