package com.study.multimodal.controller;

import com.study.multimodal.dto.ImageAnalysisRequest;
import com.study.multimodal.dto.ImageAnalysisResult;
import com.study.multimodal.dto.ImageGenerateRequest;
import com.study.multimodal.dto.ImageGenerationResult;
import com.study.multimodal.service.ImageGenerationService;
import com.study.multimodal.service.VisionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;

@RestController
@RequestMapping("/api/image")
public class ImageController {

    private final VisionService visionService;
    private final ImageGenerationService imageGenerationService;

    public ImageController(VisionService visionService,
                          ImageGenerationService imageGenerationService) {
        this.visionService = visionService;
        this.imageGenerationService = imageGenerationService;
    }

    @PostMapping("/analyze/url")
    public ResponseEntity<String> analyzeImageByUrl(@RequestBody ImageAnalysisRequest request) throws MalformedURLException {
        String result = visionService.analyzeImageByUrl(request.imageUrl(), request.question());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/analyze/upload")
    public ResponseEntity<String> analyzeImageByUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("question") String question) throws IOException {
        String result = visionService.analyzeImageByUpload(file, question);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/analyze/url/structured")
    public ResponseEntity<ImageAnalysisResult> analyzeStructured(@RequestBody ImageAnalysisRequest request) throws MalformedURLException {
        ImageAnalysisResult result = visionService.analyzeStructured(request.imageUrl());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/generate")
    public ResponseEntity<ImageGenerationResult> generateImage(@RequestBody ImageGenerateRequest request) {
        ImageGenerationResult result = imageGenerationService.generateImage(
                request.prompt(), request.quality(), request.size());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/analyze/code")
    public ResponseEntity<String> analyzeCode(@RequestParam("file") MultipartFile file) throws IOException {
        String result = visionService.analyzeCode(file);
        return ResponseEntity.ok(result);
    }
}
