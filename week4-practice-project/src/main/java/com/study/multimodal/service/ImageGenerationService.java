package com.study.multimodal.service;

import com.study.multimodal.dto.ImageGenerationResult;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class ImageGenerationService {

    private final ImageModel imageModel;

    public ImageGenerationService(ImageModel imageModel) {
        this.imageModel = imageModel;
    }

    public ImageGenerationResult generateImage(String prompt, String quality, String size) {
        int[] dimensions = parseSize(size);
        OpenAiImageOptions options = OpenAiImageOptions.builder()
                .quality(quality != null ? quality : "standard")
                .width(dimensions[0])
                .height(dimensions[1])
                .build();
        ImagePrompt imagePrompt = new ImagePrompt(prompt, options);

        ImageResponse response = imageModel.call(imagePrompt);
        String imageUrl = Objects.requireNonNull(response.getResult()).getOutput().getUrl();

        return new ImageGenerationResult(imageUrl, prompt);
    }

    private int[] parseSize(String size) {
        if (size == null || size.isEmpty()) {
            return new int[]{1024, 1024};
        }

        return switch (size.toLowerCase()) {
            case "1024x1792" -> new int[]{1024, 1792};
            case "1792x1024" -> new int[]{1792, 1024};
            default -> new int[]{1024, 1024};
        };
    }
}
