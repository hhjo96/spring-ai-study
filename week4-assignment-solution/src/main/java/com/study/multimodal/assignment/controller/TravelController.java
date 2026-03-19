package com.study.multimodal.assignment.controller;

import com.study.multimodal.assignment.dto.*;
import com.study.multimodal.assignment.service.TravelService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/travel")
public class TravelController {

    private final TravelService travelService;

    public TravelController(TravelService travelService) {
        this.travelService = travelService;
    }

    /**
     * POST /api/travel/analyze-photo
     * Vision API를 사용하여 여행 사진을 분석합니다.
     */
    @PostMapping("/analyze-photo")
    public ResponseEntity<?> analyzePhoto(@RequestBody PhotoAnalysisRequest request) {
        try {
            String analysis = travelService.analyzePhoto(request);
            return ResponseEntity.ok(new PhotoAnalysisResponse(analysis));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * POST /api/travel/analyze-photo/upload
     * Vision API를 사용하여 업로드된 여행 사진을 분석합니다.
     */
    @PostMapping("/analyze-photo/upload")
    public ResponseEntity<?> analyzePhotoByUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "language", defaultValue = "한국어") String language) {
        try {
            String analysis = travelService.analyzePhotoByUpload(file, language);
            return ResponseEntity.ok(new PhotoAnalysisResponse(analysis));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * POST /api/travel/generate-image
     * DALL-E를 사용하여 여행지 이미지를 생성합니다. (OpenAI 전용)
     */
    @PostMapping("/generate-image")
    public ResponseEntity<?> generateImage(@RequestBody ImageGenerateRequest request) {
        try {
            String imageUrl = travelService.generateImage(request);
            return ResponseEntity.ok(new ImageGenerateResponse(imageUrl));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * POST /api/travel/audio-guide
     * TTS를 사용하여 음성 여행 가이드를 생성합니다. (OpenAI 전용)
     */
    @PostMapping("/audio-guide")
    public ResponseEntity<?> generateAudioGuide(@RequestBody AudioGuideRequest request) {
        try {
            byte[] audioData = travelService.generateAudioGuide(request);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("audio/mpeg"))
                    .contentLength(audioData.length)
                    .body(audioData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }
}
