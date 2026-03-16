package com.study.multimodal.service;

import com.study.multimodal.dto.ImageAnalysisResult;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.UrlResource;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;

@Service
public class VisionService {

    private final ChatClient chatClient;

    public VisionService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public String analyzeImageByUrl(String imageUrl, String question) throws MalformedURLException {
        UrlResource imageResource = new UrlResource(URI.create(imageUrl));
        String mimeType = detectMimeType(imageUrl);

        return chatClient
                .prompt()
                .user(userSpec -> userSpec
                        .text(question)
                        .media(MimeTypeUtils.parseMimeType(mimeType), imageResource))
                .call()
                .content();
    }

    public String analyzeImageByUpload(MultipartFile file, String question) throws IOException {
        byte[] imageData = file.getBytes();
        String contentType = file.getContentType() != null ? file.getContentType() : "image/png";

        return chatClient
                .prompt()
                .user(userSpec -> userSpec
                        .text(question)
                        .media(MimeTypeUtils.parseMimeType(contentType), new ByteArrayResource(imageData)))
                .call()
                .content();
    }

    public ImageAnalysisResult analyzeStructured(String imageUrl) throws MalformedURLException {
        UrlResource imageResource = new UrlResource(URI.create(imageUrl));
        String mimeType = detectMimeType(imageUrl);

        return chatClient
                .prompt()
                .user(userSpec -> userSpec
                        .text("""
                                이 이미지를 분석하고 다음 정보를 제공해주세요:
                                - description: 이미지에 대한 간단한 설명
                                - objects: 이미지에서 발견된 주요 객체들의 리스트
                                - mood: 이미지의 전체적인 분위기
                                - colors: 이미지의 주요 색상들의 리스트
                                - tags: 이미지를 설명하는 태그들의 리스트
                                """)
                        .media(MimeTypeUtils.parseMimeType(mimeType), imageResource))
                .call()
                .entity(ImageAnalysisResult.class);
    }

    private String detectMimeType(String imageUrl) {
        String lowerCaseUrl = imageUrl.toLowerCase();
        if (lowerCaseUrl.endsWith(".jpg") || lowerCaseUrl.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerCaseUrl.endsWith(".webp")) {
            return "image/webp";
        }
        return "image/png";
    }

    public String analyzeCode(MultipartFile file) throws IOException {
        byte[] imageData = file.getBytes();
        String contentType = file.getContentType() != null ? file.getContentType() : "image/png";

        return chatClient
                .prompt()
                .user(userSpec -> userSpec
                        .text("""
                                이 코드 스크린샷을 분석하고 다음 정보를 한국어로 제공해주세요:
                                1. 프로그래밍 언어
                                2. 코드 요약
                                3. 잠재적인 문제점 또는 개선 사항
                                4. 주요 함수/메서드 식별
                                """)
                        .media(MimeTypeUtils.parseMimeType(contentType), new ByteArrayResource(imageData)))
                .call()
                .content();
    }
}
