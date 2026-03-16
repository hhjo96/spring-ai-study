package com.study.multimodal.dto;

import java.util.List;

public record ImageAnalysisResult(
        String description,
        List<String> objects,
        String mood,
        List<String> colors,
        List<String> tags
) {
}
