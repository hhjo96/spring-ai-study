package com.study.springai.dto;

public record ToolChatRequest(
        String question,
        String toolType  // "datetime", "weather", "booking"
) {
    public ToolChatRequest {
        if (toolType == null || toolType.isBlank()) toolType = "datetime";
    }
}
