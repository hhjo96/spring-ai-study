# Chapter 6 비전 및 이미지 생성

## 비전과 멀티모달 LLM

컴퓨터 비전은 기계가 주변 세계를 ‘보고’ 해석하도록 만드는 기술

단순히 이미지를 인식하는 것을 넘어서, 객체를 식별하고, 장면의 의미를 파악하며, 상황을 예측하고 대응하는 능력을 갖추게 하는 것이 궁극적인 목표

이전에는 복잡한 영상 처리 전문 기술이 필요했지만 멀티모달 LLM이 등장하면서 매우 쉽게 비전 기능을 사용할 수 있게 됨

### 멀티모달

- 텍스트, 이미지, 오디오, 비디오 등 다양한 형태의 데이터를 동시에 입력받고 통합적으로 처리할 수 있는 AI 모델의 능력을 의미

LLM과 텍스트만 소통하던 시대에서 인간처럼 보고, 듣고, 말하고, 자체적으로 상황을 예측하는 비전 기능이 추가된 멀티모달 LLM으로 빠르게 진화 중

## Spring AI멀티모달 지원

Spring AI는 멀티모달 LLM을 활용할 수 있도록, 사용자 메시지와 AI 메시지에 미디어를 포함할 수 있도록 설계, 이를 통해 텍스트뿐만 아니라 음성, 이미지 등 다양한 형태의 데이터를 입력하거나 출력할 수 있습니다.

```java
UserMessage userMessage = UserMessage.builder()
			.text(question)
			.media(media, ...)
			.build();
```

Media는 다양한 리소스로부터 생성할 수 있습니다. 예를 들어 URL, 파일, 바이너리 데이터로부터 생성 가능합니다.

```java
Media media = new Media(MimeType mimeType, URL url);
Media media = new Media(MimeType mimeType, Resource resource);
```

MimeType은 MimeTypeUtils의 상수를 사용하거나, MimeType.valueOf() 정적 메소드를 사용하여 문자열로부터 얻을 수 있습니다. 예를 들어 이미지 파일이 PNG 타입일 경우, MimeType은 다음과 같이 얻을 수 있습니다.

```java
MimeType mimeType = MimeTypeUtils.IMAGE_PNG;
MimeType mimeType = MimeType.valueOf("image/png");
```

Resource 객체는 자원의 위치 및 형태에 따라 다음과 같이 생성할 수 있습니다.

```java
Resource resource = new UrlResource(URL url);
Resource resource = new FileSystemResource(File file);
Resource resource = new CalssPathResource(String path);
Resource resource = new ByteArrayResource(byte[] byteArray);
Resource resource = new InputStreamResource(InputStream inputStream);
```

## 객체 탐지 및 상태 분석

AI가 시작적 정보를 이해하기 위해 가장 기본적으로 수행하는 작업 중 하나는 개체 탐지
개체 탐지는 이미지나 영상 속에서 어떤 객체가 어디에 위치해 있는지를 찾아내는 기술
객체의 존재 유무 파악을 넘어, 해당 객체의 종류와 정확한 위치를 함께 제공할 수 있음
예를 들어 교통 영상에서 자동차, 보행자, 신호등을 실시간으로 인식하고 각각의 위치를 표시하는 것이 객체 탐지의 대표적인 사례
객체 탐지를 위해 다양한 딥러닝 모델이 개발되었고 대표적인 모델에는 YOLO, SSD, Faster R-CNN 이 있음

상태 분석은 탐지된 객체가 어떤 상황에 놓여 있는지를 더 깊이 이해하려는 시도
예를 들어, 단순히 ‘사람’이라는 객체를 탐지하는 것을 넘어서, 그 사람이 앉아 있는지? 걷고 있는지? 위험한 상황에 처해 있는지? 등을 파악하는 것이 상태 분석

이미지 속 객체를 탐지하고 그 상태를 분석하도록 요청한 뒤, 결과를 텍스트 형태로 받아 요청자에게 전달하는 방법

AiService

```java
@Autowired
private ImageModel imageModel;
```

- ImageModel은 이미지 생성형 모델을 사용하기 위한 인터페이스, build.gradle에서 OpenAI 스타터를 의존성으로 추가하면 ImageModel의 구현 클래스인 OpenAiImageModel이 Spring 빈으로 자동 생성되기 때문에 필드 주입 가능

```java
public Flux<String> imageAnalysis(String question, String contentType, byte[] bytes) {
    // 시스템 메시지 생성
    SystemMessage systemMessage = SystemMessage.builder()
        .text("""
          당신은 이미지 분석 전문가입니다.
          사용자 질문에 맞게 이미지를 분석하고 답변을 한국어로 하세요. 
        """)
        .build();

    // 미디어 생성
    Media media = Media.builder()
        .mimeType(MimeType.valueOf(contentType))
        .data(new ByteArrayResource(bytes))
        .build();

    // 사용자 메시지 생성
    UserMessage userMessage = UserMessage.builder()
        .text(question)
        .media(media)
        .build();

    // 프롬프트 생성
    Prompt prompt = Prompt.builder()
        .messages(systemMessage, userMessage)
        .build();

    // LLM에 요청하고, 응답받기
    Flux<String> flux = chatClient.prompt(prompt)
        .stream()
        .content();
    return flux;
  }
```

- 사용자의 텍스트 질문, 이미지 MIME 타입 그리고 이미지 데이터를 받기 위한 매개변수
- UserMessage를 생성할 때 텍스트 질문과 Media를 포함 (멀티모달리티)
- 시스템 메시지와 사용자 메시지를 포함해서 Prompt 생성
- LLM에 요청 후 Flux<String> 형태의 비동기 스트림 텍스트로 응답을 수신하고 이를 그대로 반환

AiController

```java
@PostMapping(
  value = "/image-analysis",
  consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
  produces = MediaType.APPLICATION_NDJSON_VALUE
)
public Flux<String> imageAnalysis(
  @RequestParam("question") String question, 
  @RequestParam(value="attach", required = false) MultipartFile attach) throws IOException {
  // 이미지가 업로드 되지 않았을 경우
  if (attach == null || !attach.getContentType().contains("image/")) {
    Flux<String> response = Flux.just("이미지를 올려주세요.");
    return response;
  }

  Flux<String> flux = aiService.imageAnalysis(question, attach.getContentType(), attach.getBytes());
  return flux;
}  
```

- 멀티파트 폼으로 받기 위해 consumes에 multipart/form-data 형식 지정그리고 비동기 스트림 응답을 보내기 때문에 produces에는 application/x-ndjson 형식 지정
- 사용자로부터 텍스트 질문과 이미지 파일 정보를 매개값으로 받고 비동기 스트림 텍스트 응답인 Flux<String>을 반환

## 비디오 프레임 분석

하나의 이미지는 멀티모달 LLM에 입력되기 전에 먼저 비전 인코더를 통해 고차원 벡터로 변환되며, 이렇게 변환된 벡터는 텍스트 질문과 함께 LLM의 입력으로 사용됩니다.
비디오는 이러한 이미지들이 시간에 따라 연속적으로 구성된 것입니다.

비디오의 연속된 프레임을 LLM의 입력으로 제공할 경우, 입력랑의 급격한 증가로 인해 연산량과 메모리 사용량도 기하급수적으로 늘어남 따라서 대부분의 멀티모달 LLM은 비디오 전체보다는 하나의 프레임 분석에 최적화되어 있으므로, 특정 시점의 프레임만을 추출하여 LLM에게 분석 요청하는 방식이 효율적

사용자가 질문하는 시점에 카메라에서 프레임을 추출하고 이를 이미지 데이터로 변환하여 LLM에 분석 요청

## 이미지 생성형 모델

이미지 생성형 모델은 사용자가 입력한 텍스트 프롬프트를 바탕으로 새로운 이미지를 자동으로 만들어주는 모델을 말합니다.
이 모델은 딥러닝 기반의 ‘확산 모델(Diffusion model)’ 아키텍처를 가지고 수백만 장 이상의 이미지와 그에 대응하는 설명 텍스트로 학습을 했습니다.

대표 모델 : Open AI의 DALL.E 시리즈와 Stability AI의 Stable Diffusion

## OpenAI 이미지 생성형 모델

build.gradle 파일에서 OpenAI 스타터를 의존성으로 추가하면, ImageModel 인터페이스를 구현한 OpenAiImageModel이 Spring 빈으로 자동 생성되고 기본적으로 DALL.E3 모델을 사용함

이미지 생성 옵션 키워드 : model, prompt, size, n, response_format, moderation, output_compression, output_format, quality, background, style

새로운 이미지 생성 뿐만 아니라, 원본 이미지를 편집하는 기능도 포함됨

OpenAI는 생성, 편집, 변형 이렇게 세 가지 목적으로 이미지 생성형 모델 사용 가능

이미지 편집 옵션 키워드 : model, image, prompt, size, n, quality, mask, background

## Spring AI 이미지 생성형 모델 지원

Spring Image Model API는 여러 이미지 생성 모델을 추상화하여, Spring 기반 앱이 쉽게 이미지를 생성할 수 있도록 도와줌

Spring Image Model API는 ImageModel 인터페이스를 기반으로 구성되며, 이를 구현한 모델별 클래스를 통해 실제 이미지 생성을 수행

생성된 이미지는 ImageGeneration 객체로 반환되며, 이 객체에는 생성된 이미지의 URL 또는 Base64로 인코딩된 이미지 문자열이 포함됨

### ImageModel 인터페이스

- 이미지 생성형 모델의 클라이언트, 모델을 호출하는 call() 메소드 선언되어 있음

### ImagePrompt 클래스

- 이미지 생성형 모델의 입력 데이터를 캡슐화, 이미지 생성에 사용할 텍스트(List<ImageMessage>) 및 모델 옵션(ImageOptions)

### ImageMessage 클래스

- 이미지 생성에 사용할 텍스트와 해당 텍스트가 이미지 생성에 미치는 영향력을 나타내는 가중치를 캡슐화, 가중치의 기본값 0이고 음수 및 양수를 가질 수 있음

### ImageOptions 인터페이스

- 이미지 생성 모델에 전달할 수 있는 옵션을 설정
- 모델 이름, 이미지 폭과 높이, 생성 개수, 결과 형태 설정 가능
- 결과 형태는 생성된 이미지를 다운로드할 수 있는 URL 또는 Base64로 인코딩된 이미지 문자열 지정 가능

### ImageResponse 클래스

- 이미지 생성형 모델의 출력을 저장
- 여러 장의 이미지 각각은 ImageGeneration으로 저장됨.
- 모델 출력에 대한 메타데이터도 저장

### ImageGeneration 클래스

- 생성된 이미지와 해당 이미지에 대한 메타데이터 저장

## 이미지 생성

사용 모델 : GPT-Image-1

AiService

```java
// ##### 한글 문장을 영어 문장으로 번역하는 메소드 #####
private String koToEn(String text) {
  String question = """
        당신은 번역사입니다. 아래 한글 문장을 영어 문장으로 번역해주세요.
        %s
      """.formatted(text);

  // UserMessage 생성
  UserMessage userMessage = UserMessage.builder()
      .text(question)
      .build();

  // Prompt 생성
  Prompt prompt = Prompt.builder()
      .messages(userMessage)
      .build();

  // LLM을 호출하고 텍스트 답변 얻기
  String englishDescription = chatClient.prompt(prompt).call().content();
  return englishDescription;
}
```

```java
// ##### 이미지를 새로 생성하는 메소드 #####
  public String generateImage(String description) {
    // 한글 질문을 영어 질문으로 번역
    String englishDescription = koToEn(description);

    // 이미지 설명을 포함하는 ImageMessage 생성
    ImageMessage imageMessage = new ImageMessage(englishDescription);

    // gpt-image-1 옵션 설정
    OpenAiImageOptions imageOptions = OpenAiImageOptions.builder()
        .model("gpt-image-1")
        .quality("low")
        .width(1536)
        .height(1024)
        .N(1)
        .build();
  
    // dall-e 시리즈 옵션 설정
    // OpenAiImageOptions imageOptions = OpenAiImageOptions.builder()
    //     // dall-e 시리즈 옵션
    //     .model("dall-e-3")
    //     .responseFormat("b64_json")
    //     .width(1024)
    //     .height(1024)
    //     .N(1)
    //     .build();        

    // 프롬프트 생성
    List<ImageMessage> imageMessageList = List.of(imageMessage);
    ImagePrompt imagePrompt = new ImagePrompt(imageMessageList, imageOptions);

    // 모델 호출 및 응답 받기
    ImageResponse imageResponse = imageModel.call(imagePrompt);

    // base64로 인코딩된 이미지 문자열 얻기
    String b64Json = imageResponse.getResult().getOutput().getB64Json();
    return b64Json;
  }
```

- 사용자가 올린 한글 이미지 설명을 영어로 번역하고 ImageMessage 생성
- OpenAiImageOptions 옵션 설정
- ImageMessage와 OpenAiImageOptions를 가지고 ImagePrompt 생성
- ImageModel call() 메소드 호출하고 ImageResponse 반환
- Base64로 인코딩된 이미지 문자열을 추출하고 반환

AiController

```java
@PostMapping(
  value = "/image-generate",
  consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
  produces = MediaType.TEXT_PLAIN_VALUE
)
public String imageGenerate(@RequestParam("description") String description) {
  try {
    String b64Json = aiService.generateImage(description);
    return b64Json;
  } catch(Exception e) {
    e.printStackTrace();
    return "Error: " + e.getMessage();
  }
}
```

- 응답은 Base64로 인코딩된 이미지 문자열이므로 text/plain으로 설정

## 이미지 편집

Spring AI에서는 이미지 편집 기능을 위한 API를 제공하지 않음.

OpenAI Java SDK를 추가하여서 구현하거나 책에서 소개하는 방식처럼 OpenAI API를 직접 이용해서 구현해야 함. 아래 코드 참고

```java
// ##### 원본 이미지를 편집하는 메소드 #####
public String editImage(String description, byte[] originalImage, byte[] maskImage) {
  // 한글 질문을 영어 질문으로 번역
  String englishDescription = koToEn(description);

  // 원본 이미지를 ByteArrayResource로 생성
  ByteArrayResource originalRes = new ByteArrayResource(originalImage) {
    @Override
    public String getFilename() {
      return "image.png"; // 가상 파일 이름 반환(확장명으로 타입 정보 획득)
    }
  };

  // 마스크 이미지를 ByteArrayResource로 생성
  ByteArrayResource maskRes = new ByteArrayResource(maskImage) {
    @Override
    public String getFilename() {
      return "mask.png"; // 가상 파일 이름 반환
    }
  };

  // 이미지 모델 옵션 설정
  MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
  form.add("model", "gpt-image-1");
  form.add("image", originalRes);
  form.add("mask", maskRes);
  form.add("prompt", englishDescription);
  form.add("n", "1");
  form.add("size", "1536x1024");
  form.add("quality", "high");

  // WebClient 생성
  WebClient webClient = WebClient.builder()
      // 이미지 편집을 위한 요청 URL
      .baseUrl("https://api.openai.com/v1/images/edits")
      // 인증 헤더 설정
      .defaultHeader("Authorization", "Bearer " + System.getenv("OPENAI_API_KEY"))
      // 전략을 적용해서 메모리를 늘림
      .exchangeStrategies(ExchangeStrategies.builder()
        .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(10 * 1536 * 1024))
        .build())
      .build();

  // 비동기 단일값(OpenAIImageEditResponse) 스트림인 Mono 얻기
  Mono<OpenAIImageEditResponse> mono = webClient.post()
      // multipart/form-data 형식으로 전송
      .contentType(MediaType.MULTIPART_FORM_DATA)
      // 요청 본문에 form 데이터를 넣음
      .body(BodyInserters.fromMultipartData(form))
      // 응답 받기
      .retrieve()
      // 응답 본문의 JSON을 OpenAIImageEditResponse 타입으로 역직렬화해서
      // 비동기 단일값(OpenAIImageEditResponse) 스트림인 Mono로 반환
      .bodyToMono(OpenAIImageEditResponse.class);

  // Mono가 완료될 때까지 현재 스레드를 블로킹하고,
  // 동기 방식으로 단일값 OpenAIImageEditResponse를 얻음
  OpenAIImageEditResponse response = mono.block();

  // 레코드로부터 base64로 인코딩된 이미지 문자열 얻기
  String b64Json = response.data().get(0).b64_json();
  return b64Json;

  // 클래스로부터 base64로 인코딩된 이미지 문자열 얻기
  // String b64Json = response.getData().get(0).getB64_json();
  // return b64Json;
}

// 레코드로 역직렬화할 경우
// {"data": [{"url": "xxxxx", "b64_json": "xxxxx"}, ... ]}
// 선언된 필드 외에 JSON에 포함된 속성들을 무시
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenAIImageEditResponse(List<Image> data) {
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Image(
      String url,
      String b64_json) {
  }
}
```