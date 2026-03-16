# Week 4 Practice Project — Spring AI 멀티모달 애플리케이션

Spring AI의 멀티모달 기능을 익히는 실습 프로젝트입니다.

## 기술 스택

- Java 17, Spring Boot 4.0.2, Spring AI 2.0.0-M2
- OpenAI (`gpt-4.1-nano`, `dall-e-3`, `tts-1`, `whisper-1`)
- Thymeleaf 웹 UI

---

## 이론

| 주제 | 핵심 개념 |
|------|-----------|
| Vision API | ChatClient + `.media()`, URL/업로드 이미지 분석, Structured Output |
| 이미지 생성 | ImageModel 추상화, DALL-E 3, `OpenAiImageOptions` 빌더 |
| TTS (Text-to-Speech) | `OpenAiAudioSpeechModel`, `TextToSpeechPrompt`, voice·speed·format 옵션 |
| STT (Speech-to-Text) | `OpenAiAudioTranscriptionModel`, `AudioTranscriptionPrompt`, Whisper |
| AI 파이프라인 | Chat → TTS 체이닝으로 음성 비서 구현 |

---

## 실습 내용

### 1. Vision API — 이미지 분석 (`VisionService`)
- URL 이미지 분석: `UrlResource` + `chatClient.prompt().user(userSpec.text().media())` 패턴
- 업로드 이미지 분석: `MultipartFile` → `ByteArrayResource` + `MimeTypeUtils.parseMimeType()`
- 구조화된 응답: `.entity(ImageAnalysisResult.class)` 로 JSON → Java Record 자동 매핑
- 코드 스크린샷 분석: 이미지에서 프로그래밍 언어·요약·개선점 추출

### 2. 이미지 생성 — DALL-E 3 (`ImageGenerationService`)
- `OpenAiImageOptions.builder()` 로 크기(1024×1024 등)·품질(standard/hd) 설정
- `ImagePrompt` → `imageModel.call()` → `ImageResponse`에서 URL 추출
- 크기 파싱 로직으로 small/medium/large 문자열 → 실제 픽셀 크기 변환

### 3. 음성 처리 — TTS/STT (`SpeechService`)
- TTS: `OpenAiAudioSpeechOptions`로 voice·speed·format 설정 → `TextToSpeechPrompt` → MP3 byte[]
- STT: `OpenAiAudioTranscriptionOptions` → `AudioTranscriptionPrompt` → 텍스트 변환
- 음성 비서: `chatClient.prompt().user()` 로 텍스트 생성 후 바로 TTS로 변환하는 파이프라인

---

## 실행 방법

```bash
# 방법 1: OpenAI (전체 기능)
export OPENAI_API_KEY=sk-your-key-here
./gradlew bootRun
```

실행 후 http://localhost:8080 에서 웹 UI로 모든 기능을 테스트할 수 있습니다.

---