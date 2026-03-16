# AI 여행 가이드 (AI Travel Guide)

## 개요

Spring AI를 활용한 지능형 여행 안내 시스템을 구현하는 과제입니다.
이 과제를 통해 실습에서 배운 Vision API, DALL-E 이미지 생성, TTS 음성 합성을 하나의 도메인에 적용합니다.

## 학습 목표

1. **Vision API 활용**: ChatClient + Media로 이미지를 분석하고 여행지 정보를 추출
2. **이미지 생성 API**: ImageModel + OpenAiImageOptions로 텍스트 프롬프트에서 여행지 이미지 생성
3. **TTS 음성 합성**: OpenAiAudioSpeechModel + TextToSpeechPrompt로 텍스트를 음성으로 변환
4. **파이프라인 패턴**: ChatClient → TTS 체이닝으로 음성 여행 가이드 생성
5. **멀티모달 통합**: 여러 AI 모델을 하나의 서비스에서 조합하여 사용

## 기술 스택

- **Spring Boot** 4.0.2 / **Spring AI** 2.0.0-M2
- **Java** 17
- **OpenAI Models**: gpt-4.1-nano (Vision), dall-e-3 (이미지 생성), tts-1 (TTS)

## 프로젝트 구조

```
src/main/java/com/study/multimodal/assignment/
├── AssignmentApplication.java          # Spring Boot 진입점
├── config/
│   └── AiConfig.java                
├── controller/
│   └── TravelController.java          # REST API 엔드포인트 (완성)
├── service/
│   └── TravelService.java             # 비즈니스 로직 (TODO - 구현 대상)
└── dto/
    ├── PhotoAnalysisRequest.java       # 여행 사진 분석 요청 DTO (완성)
    ├── ImageGenerateRequest.java       # 이미지 생성 요청 DTO (완성)
    └── AudioGuideRequest.java          # 음성 가이드 요청 DTO (완성)

src/main/resources/
├── application.yml                     # 애플리케이션 설정 (완성)
└── templates/
    └── index.html                      # 웹 UI (완성)
```

## 구현 과제

`TravelService.java`의 TODO 4개를 구현하세요. 컨트롤러, DTO, 프론트엔드는 모두 완성되어 있으므로, 서비스 로직만 작성하면 바로 동작합니다.

### TODO 1: 여행 사진 URL 분석 (Vision API)
**파일**: `TravelService.java` → `analyzePhoto()` 메서드

- **목표**: ChatClient의 `.user(userSpec -> ...)` 패턴으로 이미지 URL을 Vision API에 전달하여 분석
- **입력**: PhotoAnalysisRequest (imageUrl, language)
- **출력**: 분석 결과 텍스트 (String)
- **핵심 학습**: `UrlResource`로 이미지 URL 래핑, `.media()` 메서드로 이미지 첨부

### TODO 2: 여행 사진 업로드 분석 (Vision API + MultipartFile)
**파일**: `TravelService.java` → `analyzePhotoByUpload()` 메서드

- **목표**: 업로드된 이미지 파일을 ByteArrayResource로 변환하여 Vision API로 분석
- **입력**: MultipartFile (이미지 파일), String (language)
- **출력**: 분석 결과 텍스트 (String)
- **핵심 학습**: `ByteArrayResource`로 바이트 배열 래핑, `MimeTypeUtils.parseMimeType()`으로 MIME 타입 처리

### TODO 3: 여행지 이미지 생성 (DALL-E 3)
**파일**: `TravelService.java` → `generateImage()` 메서드

- **목표**: destination, style, season을 조합한 프롬프트로 DALL-E 3 이미지를 생성
- **입력**: ImageGenerateRequest (destination, style, season)
- **출력**: 생성된 이미지 URL (String)
- **핵심 학습**: `ImagePrompt` + `OpenAiImageOptions.builder()` → `imageModel.call()` 패턴

### TODO 4: 음성 여행 가이드 (Chat + TTS 파이프라인)
**파일**: `TravelService.java` → `generateAudioGuide()` 메서드

- **목표**: ChatClient로 가이드 텍스트 생성 → TTS로 음성 변환하는 파이프라인 구현
- **입력**: AudioGuideRequest (destination, highlights, voice)
- **출력**: MP3 바이트 배열 (byte[])
- **핵심 학습**: `TextToSpeechPrompt` + `OpenAiAudioSpeechOptions.builder()` → `speechModel.call()` 패턴, 두 모델 체이닝

## 설정 및 실행

### 방법 1: OpenAI로 실행 (기본, 모든 TODO 테스트 가능)

```bash
# 1. API Key 설정
export OPENAI_API_KEY=sk-your-api-key-here

# 2. 실행
./gradlew bootRun

# 3. 브라우저 접속
# http://localhost:8080
```

TODO 구현 후 웹 UI의 각 탭에서 기능을 테스트할 수 있습니다.

## API 엔드포인트

### 1. 여행 사진 URL 분석
```
POST /api/travel/analyze-photo
Content-Type: application/json

{ "imageUrl": "https://example.com/image.jpg", "language": "한국어" }
→ { "analysis": "분석 결과 텍스트..." }
```

### 2. 여행 사진 업로드 분석
```
POST /api/travel/analyze-photo/upload
Content-Type: multipart/form-data

file: (이미지 파일), language: "한국어"
→ { "analysis": "분석 결과 텍스트..." }
```

### 3. 여행지 이미지 생성
```
POST /api/travel/generate-image
Content-Type: application/json

{ "destination": "파리", "style": "사실적", "season": "봄" }
→ { "imageUrl": "https://oaidalleapiprodscus.blob.core.windows.net/..." }
```

### 4. 음성 여행 가이드
```
POST /api/travel/audio-guide
Content-Type: application/json

{ "destination": "파리", "highlights": "에펠탑, 루브르 박물관, 센강 크루즈", "voice": "nova" }
→ Binary audio/mpeg data
```

## 주의사항

- OpenAI 모드: API 키가 필요합니다 (DALL-E, TTS 호출 시 비용 발생)
- 이미지 분석은 공개 접근 가능한 URL의 이미지를 사용해야 합니다
- DALL-E 3 이미지 생성은 건당 약 10~30초 소요됩니다
- 실습 중 `429 Too Many Requests` 에러가 발생하면 잠시 후 재시도하세요

## 참고 자료

- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- [OpenAI API Reference](https://platform.openai.com/docs/api-reference)