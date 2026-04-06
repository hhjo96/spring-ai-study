package com.study.springai.dto;

public record BookingChatRequest(
        String question,
        String userName
) {
    public BookingChatRequest {
        if (userName == null || userName.isBlank()) userName = "사용자";
    }
}
