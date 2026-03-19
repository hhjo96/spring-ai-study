package com.study.multimodal.assignment.service;

import com.study.multimodal.assignment.dto.AudioGuideRequest;
import com.study.multimodal.assignment.dto.ImageGenerateRequest;
import com.study.multimodal.assignment.dto.PhotoAnalysisRequest;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.OpenAiAudioSpeechOptions;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.UrlResource;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Objects;

@Service
public class TravelService {

    private final ChatClient chatClient;
    private final ImageModel imageModel;
    private final OpenAiAudioSpeechModel speechModel;

    public TravelService(ChatClient chatClient,
                        ImageModel imageModel,
                        OpenAiAudioSpeechModel speechModel) {
        this.chatClient = chatClient;
        this.imageModel = imageModel;
        this.speechModel = speechModel;
    }

    /**
     * TODO 1: Vision API로 여행 사진(URL)을 분석합니다.
     */
    public String analyzePhoto(PhotoAnalysisRequest request) throws MalformedURLException {
        UrlResource imageResource = new UrlResource(URI.create(request.imageUrl()));
        String mimeType = detectMimeType(request.imageUrl());

        return chatClient
                .prompt()
                .user(userSpec -> userSpec
                        .text("이 여행 사진을 " + request.language() + "로 분석해주세요. "
                                + "장소, 분위기, 추천 활동을 포함해서 설명해주세요.")
                        .media(MimeTypeUtils.parseMimeType(mimeType), imageResource))
                .call()
                .content();
    }

    /**
     * TODO 2: Vision API로 업로드된 여행 사진을 분석합니다.
     */
    public String analyzePhotoByUpload(MultipartFile file, String language) throws IOException {
        byte[] imageData = file.getBytes();
        String contentType = file.getContentType() != null ? file.getContentType() : "image/png";

        return chatClient
                .prompt()
                .user(userSpec -> userSpec
                        .text("이 여행 사진을 " + language + "로 분석해주세요. "
                                + "장소, 분위기, 추천 활동을 포함해서 설명해주세요.")
                        .media(MimeTypeUtils.parseMimeType(contentType), new ByteArrayResource(imageData)))
                .call()
                .content();
    }

    /**
     * TODO 3: DALL-E로 여행지 이미지를 생성합니다. (OpenAI 전용)
     */
    public String generateImage(ImageGenerateRequest request) {
        String prompt = request.season() + "의 " + request.destination() + ", "
                + request.style() + " 스타일의 아름다운 여행지 풍경 이미지";

        OpenAiImageOptions options = OpenAiImageOptions.builder()
                .height(1024)
                .width(1024)
                .quality("standard")
                .build();

        ImagePrompt imagePrompt = new ImagePrompt(prompt, options);
        ImageResponse response = imageModel.call(imagePrompt);

        return Objects.requireNonNull(response.getResult()).getOutput().getUrl();
    }

    /**
     * TODO 4: ChatClient + TTS 파이프라인으로 음성 여행 가이드를 생성합니다. (OpenAI 전용)
     */
    public byte[] generateAudioGuide(AudioGuideRequest request) {
        // Step 1: ChatClient로 여행 가이드 텍스트 생성
        String guideText = chatClient
                .prompt()
                .user(request.destination() + " 여행 가이드를 작성해주세요. "
                        + "주요 명소: " + request.highlights()
                        + ". 각 명소에 대한 간단한 설명과 여행 팁을 포함해주세요.")
                .call()
                .content();

        // Step 2: TTS로 텍스트를 음성으로 변환
        OpenAiAudioSpeechOptions speechOptions = OpenAiAudioSpeechOptions.builder()
                .voice(request.voice() != null ? request.voice() : "nova")
                .speed(1.0)
                .responseFormat(OpenAiAudioApi.SpeechRequest.AudioResponseFormat.MP3)
                .build();

        TextToSpeechPrompt speechPrompt = new TextToSpeechPrompt(guideText, speechOptions);
        return speechModel.call(speechPrompt).getResult().getOutput();
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
}
