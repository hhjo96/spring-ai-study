package com.study.multimodal.controller;

import com.study.multimodal.dto.SpeechRequest;
import com.study.multimodal.dto.TtsRequest;
import com.study.multimodal.service.SpeechService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/speech")
public class SpeechController {

    private final SpeechService speechService;

    public SpeechController(SpeechService speechService) {
        this.speechService = speechService;
    }

    @PostMapping("/tts")
    public ResponseEntity<byte[]> textToSpeech(@RequestBody TtsRequest request) {
        byte[] audioData = speechService.textToSpeech(request.text(), request.voice());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"speech.mp3\"")
                .body(audioData);
    }

    @PostMapping("/stt")
    public ResponseEntity<String> speechToText(@RequestParam("file") MultipartFile file) throws IOException {
        String transcript = speechService.speechToText(file);
        return ResponseEntity.ok(transcript);
    }

    @PostMapping("/assistant")
    public ResponseEntity<byte[]> voiceAssistant(@RequestBody SpeechRequest request) {
        byte[] audioData = speechService.voiceAssistant(request.question());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"response.mp3\"")
                .body(audioData);
    }
}
