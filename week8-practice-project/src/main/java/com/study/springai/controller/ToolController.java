package com.study.springai.controller;

import com.study.springai.dto.BookingChatRequest;
import com.study.springai.dto.ChatRequest;
import com.study.springai.dto.ChatResponse;
import com.study.springai.service.ToolCallingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tool")
@RequiredArgsConstructor
public class ToolController {

    private final ToolCallingService toolCallingService;

    /**
     * 날짜/시간 도구 채팅
     */
    @PostMapping("/datetime")
    public ResponseEntity<ChatResponse> chatWithDateTime(@RequestBody ChatRequest request) {
        String answer = toolCallingService.chatWithDateTime(request.question());
        return ResponseEntity.ok(new ChatResponse(answer));
    }

    /**
     * 날씨 도구 채팅
     */
    @PostMapping("/weather")
    public ResponseEntity<ChatResponse> chatWithWeather(@RequestBody ChatRequest request) {
        String answer = toolCallingService.chatWithWeather(request.question());
        return ResponseEntity.ok(new ChatResponse(answer));
    }

    /**
     * 회의실 예약 도구 채팅 (ToolContext 활용)
     */
    @PostMapping("/booking")
    public ResponseEntity<ChatResponse> chatWithBooking(@RequestBody BookingChatRequest request) {
        String answer = toolCallingService.chatWithBooking(request.question(), request.userName());
        return ResponseEntity.ok(new ChatResponse(answer));
    }
}
