package com.study.multimodal.assignment.service;

import com.study.multimodal.assignment.dto.AudioGuideRequest;
import com.study.multimodal.assignment.dto.ImageGenerateRequest;
import com.study.multimodal.assignment.dto.PhotoAnalysisRequest;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
     * TODO 1: Vision API로 여행 사진(URL)을 분석하세요
     *
     * - request에서 이미지 URL과 분석 언어를 꺼내 ChatClient로 멀티모달 프롬프트를 구성합니다.
     * - 이미지 URL을 Resource로 감싸서 media()에 전달해야 합니다.
     * - week4-practice-project의 VisionService.analyzeImageByUrl()을 참고하세요.
     *
     * @see org.springframework.core.io.UrlResource
     * @see org.springframework.util.MimeTypeUtils
     */
    public String analyzePhoto(PhotoAnalysisRequest request) {
        // TODO: 구현하세요
        throw new UnsupportedOperationException("TODO: 구현하세요");
    }

    /**
     * TODO 2: Vision API로 업로드된 여행 사진을 분석하세요
     *
     * - TODO 1과 동일한 멀티모달 프롬프트 구조이지만, URL 대신 MultipartFile을 처리합니다.
     * - MultipartFile → byte[] → Resource 변환이 핵심입니다.
     * - 업로드 파일의 Content-Type을 MIME 타입으로 변환하여 media()에 전달해야 합니다.
     * - IOException 처리가 필요합니다.
     *
     * @see org.springframework.core.io.ByteArrayResource
     * @see org.springframework.util.MimeTypeUtils#parseMimeType(String)
     */
    public String analyzePhotoByUpload(MultipartFile file, String language) {
        // TODO: 구현하세요
        throw new UnsupportedOperationException("TODO: 구현하세요");
    }

    /**
     * TODO 3: DALL-E로 여행지 이미지를 생성하세요
     *
     * - request의 destination, style, season을 조합하여 이미지 생성 프롬프트를 만듭니다.
     * - ChatClient가 아닌 ImageModel을 사용합니다. 옵션 빌더로 크기·품질을 지정하세요.
     * - week4-practice-project의 ImageGenerationService를 참고하세요.
     *
     * @see org.springframework.ai.image.ImagePrompt
     * @see org.springframework.ai.openai.OpenAiImageOptions
     */
    public String generateImage(ImageGenerateRequest request) {
        // TODO: 구현하세요
        throw new UnsupportedOperationException("TODO: 구현하세요");
    }

    /**
     * TODO 4: TTS로 음성 여행 가이드를 생성하세요
     *
     * - 두 AI 모델을 파이프라인으로 연결합니다:
     *   1) ChatClient → 여행 가이드 텍스트 생성
     *   2) speechModel → 텍스트를 MP3 음성으로 변환
     * - 음성 옵션(voice, speed, format)을 빌더로 구성하고 TTS 프롬프트를 만들어 호출합니다.
     * - week4-practice-project의 SpeechService.textToSpeech()를 참고하세요.
     *
     * @see org.springframework.ai.audio.tts.TextToSpeechPrompt
     * @see org.springframework.ai.openai.OpenAiAudioSpeechOptions
     */
    public byte[] generateAudioGuide(AudioGuideRequest request) {
        // TODO: 구현하세요
        throw new UnsupportedOperationException("TODO: 구현하세요");
    }
}
