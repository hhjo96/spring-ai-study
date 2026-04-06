package com.study.bookadvisor.dto;

import java.time.LocalDateTime;

public record ReviewResponse(
        Long id,
        Long bookId,
        String reviewerName,
        String content,
        Double rating,
        String source,
        LocalDateTime createdAt
) {}
