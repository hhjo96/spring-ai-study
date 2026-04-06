package com.study.bookadvisor.dto;

import java.time.LocalDate;

public record EventResponse(
        Long id,
        Long bookId,
        String eventName,
        String description,
        Integer discountPercent,
        LocalDate startDate,
        LocalDate endDate,
        Boolean active
) {}
