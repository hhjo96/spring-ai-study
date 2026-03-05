# Chapter 5 음성 대화

## 음성 변환 기술

STT(Speech-To-Text)는 사람이 말한 음성으로 텍스트로 변환하는 기술

- STT 모델은 입력된 음성을 발음 단위로 구분해서 확률적으로 높은 단어를 선별하고, 이것을 언어 모델을 통해 완전한 문장으로 보정한 뒤에 출력

TTS(Text-To-Speech)는 텍스트 문장을 자연스러운 음성으로 변환하는 기술

- TTS 모델은 입력된 텍스트를 토큰 단위로 나누고, 발음, 강세, 억양 정보를 추출 그리고 토큰을 발음 기호로 변환하고 문맥에 따라 발음을 조정 한 후, 음성 파형으로 출력

### OpenAI 모델

[STT](https://developers.openai.com/api/docs/guides/speech-to-text) : `gpt-4o-{mini}-transcribe`, `whisper-1` (온프레미스 무료 사용 가능)

[TTS](https://developers.openai.com/api/docs/guides/text-to-speech) : `gpt-4o-mini-tts`, `tts-1`

[Compare models](https://developers.openai.com/api/docs/models) : `gpt-realtime-{mini}`(WebRTC/WebSocket, 입출력 음성을 스트림 형태로 실시간 송수신), `gpt-audio-{mini}`(REST API, 음성 입력 전송 → 음성 답변 수신)

## 음성 변환

AiService

```java
private ChatClient chatClient;
private OpenAiAudioTranscriptionModel openAiAudioTranscriptionModel;
private OpenAiAudioSpeechModel openAiAudioSpeechModel;

public AiService(ChatClient.Builder chatClientBuilder,
    OpenAiAudioTranscriptionModel openAiAudioTranscriptionModel,
    OpenAiAudioSpeechModel openAiAudioSpeechModel) {
  chatClient = chatClientBuilder.build();
  this.openAiAudioTranscriptionModel = openAiAudioTranscriptionModel;
  this.openAiAudioSpeechModel = openAiAudioSpeechModel;
}
```

- 음성을 텍스트로 변환하는 OpenAiAudioTranscriptionModel과 텍스트를 음성으로 변환하는 OpenAiAudioSpeechModel을 필드로 선언

음성을 텍스트로 변환

```java
public String stt(byte[] bytes) {
	// 음성 데이터(byte[])를 ByteArrayResource로 생성
	Resource audioResource = new ByteArrayResource(bytes);
	
	// 모델 옵션 설정
	OpenAiAudioTranscriptionOptions options = OpenAiAudioTranscriptionOptions.builder()
	    .model("whisper-1")
	    .language("ko") // 입력 음성 언어의 종류 설정, 출력 언어에도 영향을 미침
	    .build();
	
	// 프롬프트 생성
	AudioTranscriptionPrompt prompt = new AudioTranscriptionPrompt(audioResource, options);
	
	// 모델을 호출하고 응답받기
	AudioTranscriptionResponse response = openAiAudioTranscriptionModel.call(prompt);
	String text = response.getResult().getOutput();
	
	return text;
}
```

- 매개값으로 받은 음성 데이터 (byte[] 배열)를 ByteArrayResource로 래핑합니다. 이 ByteArray Resource는 프롬프트를 생성할 때 사용됩니다.
- STT 모델의 옵션으로 모델명과 입력 음성의 언어를 설정합니다. 언어롤 명시하지 않으면 자동으로 감지되지만, 명시할 경우 음성의 언어를 판별하는 과정을 생략할 수 있어서 처리 속도가 다소 향상됨. 출력 텍스트는 입력 음성과 동일한 언어로 반환, language 값은 ISO 639-1 형식의 언어 코드 사용 (ko, en)
- ByteArrayRosource와 모델 옵션을가지고 AudioTranscriptionPrompt를 생성합니다.
- STT 모델에 변환 요청을 보내고 응답에서 변환된 텍스트를추출합니다.

텍스트를 음성으로 변환

```java
public byte[] tts(String text) {
  // 모델 옵션 설정
  OpenAiAudioSpeechOptions options = OpenAiAudioSpeechOptions.builder()
      .model("gpt-4o-mini-tts") // MP3가 기본 형식, 최대 입력 토큰 수 2000
      .voice(SpeechRequest.Voice.ALLOY)
      .responseFormat(SpeechRequest.AudioResponseFormat.MP3)
      .speed(1.0f)
      .build();

  // 프롬프트 생성
  SpeechPrompt prompt = new SpeechPrompt(text, options);

  // 모델을 호출하고 응답받기
  SpeechResponse response = openAiAudioSpeechModel.call(prompt);
  byte[] bytes = response.getResult().getOutput();

  return bytes;
}
```

- TTS 모델 옵션에서는 model에 gpt-4o-mini-tts를 지정하고. voice는 SpeechRequest.Voice 열거형 상수 중 하나를 선택합니다. responseFormat은 출력할오디오 형식으로. AudioResponseFormat 열거형 상수 중 하나를 설정합니다. speed는 음성 합성 속도를 의미하며, 기본값인 1.0을 지정합니다.

AiController

```java
@PostMapping(
  value = "/stt", 
  consumes = MediaType.MULTIPART_FORM_DATA_VALUE, 
  produces = MediaType.TEXT_PLAIN_VALUE
)
public String stt(@RequestParam("speech") MultipartFile speech) throws IOException {
  String text = aiService.stt(speech.getBytes());
  return text;
}

@PostMapping(
  value = "/tts", 
  consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, 
  produces = MediaType.APPLICATION_OCTET_STREAM_VALUE
)
public byte[] tts(@RequestParam("text") String text) {
  byte[] bytes = aiService.tts(text);
  return bytes;
}
```

- STT: multipart/form-data ⇒ consumes 속성에 MediaType.MULTIPART_FORM_DATA_VALUE
- TTS: produces에는 응답 본문이 바이너리 데이터이므로 MIME 타입으로
  application/octet-stream을 설정

## 텍스트도 같이 출력되는 음성 대화

### STT-LLM-TTS 조합

[음성 질문 → 텍스트 질문]은 STT 모델이 처리하고, [텍스트 질문 → 텍스트 답변]은 LLM 모델이 처리, 그리고 [텍스트 답변 → 음성 답변]은 TTS 모델이 처리

AiService

```java
public Map<String, String> chatText(String question) {
    // LLM로 요청하고, 텍스트 응답 얻기
    String textAnswer = chatClient.prompt()
        .system("50자 이내로 한국어로 답변해주세요.")
        .user(question)
        .call()
        .content();

    // TTS 모델로 요청하고 응답으로 받은 음성 데이터를 base64 문자열로 변환
    byte[] audio = tts(textAnswer);
    String base64Audio = Base64.getEncoder().encodeToString(audio);

    // 텍스트 답변과 음성 답변을 Map에 저장
    Map<String, String> response = new HashMap<>();
    response.put("text", textAnswer);
    response.put("audio", base64Audio);

    return response;
  }
```

- chatText: 텍스트 질문 → LLM 텍스트 답변 → TTS모델로 음성 답변

## 순수 음성 대화 구현 1

AiService

```java
public Flux<byte[]> ttsFlux(String text) {
  // 모델 옵션 설정
  OpenAiAudioSpeechOptions options = OpenAiAudioSpeechOptions.builder()
      .model("gpt-4o-mini-tts")
      .voice(SpeechRequest.Voice.ALLOY)
      .responseFormat(AudioResponseFormat.MP3)
      .speed(1.0f)
      .build();

  // 프롬프트 생성
  SpeechPrompt prompt = new SpeechPrompt(text, options);

  // 모델로 요청하고 응답받기
  Flux<SpeechResponse> response = openAiAudioSpeechModel.stream(prompt);
  Flux<byte[]> flux = response.map(speechResponse -> speechResponse.getResult().getOutput());
  return flux;
}
```

- ttsFlux() 메소드의 매개값은 음성으로 변환할 텍스트입니다. 이 메소드는 변환된 음성을 비동기 스트림 타입인 FIux〈byte[]〉 형태로 반환합니다.
- 음성 모델에 요청할 때 stream() 메소드를 사용했습니다. stream() 메소드의 반환 타입은 비동기 스트림 타입인 FIux〈SpeechResponse)입니다. 그러나 이 타입은 브라우저로 보내는 응답으로 사용할 수 없기 때문에 FIux〈byte[]〉 로 변환해야 합니다.

```java
public Flux<byte[]> chatVoiceSttLlmTts(byte[] audioBytes) {
  // STT를 이용해서 음성 질문을 텍스트 질문으로 변환
  String textQuestion = stt(audioBytes);

  // 텍스트 질문으로 LLM에 요청하고, 텍스트 응답 얻기
  String textAnswer = chatClient.prompt()
      .system("50자 이내로 답변해주세요.")
      .user(textQuestion)
      .call()
      .content();

  // TTS를 이용해서 비동기 음성 데이터 얻기
  Flux<byte[]> flux = ttsFlux(textAnswer);
  return flux;
}
```

- 사용자의 음성 데이터를 텍스트로 변환하기 위해 stt() 메소드를 이용합니다.
- 텍스트질문에 대해 LLM의 텍스트 답변을 받습니다.
- tsFlux() 메소드를 이용해서 텍스트 답변을비동기 스트림 음성 데이터로 변환하고반환합니다.

AiController

```java
@PostMapping(
  value = "/chat-voice-stt-llm-tts", 
  consumes = MediaType.MULTIPART_FORM_DATA_VALUE, 
  produces = MediaType.APPLICATION_OCTET_STREAM_VALUE
)
public void chatVoiceSttLlmTts(
  @RequestParam("question") MultipartFile question, 
  HttpServletResponse response) throws Exception {
  // 비동기 음성 데이터를 Flux<byte[]>을 얻기
  Flux<byte[]> flux = aiService.chatVoiceSttLlmTts(question.getBytes());

  // 음성 데이터를 응답 본문으로 스트림 출력
  OutputStream outputStream = response.getOutputStream();
  for (byte[] chunk : flux.toIterable()) {
    outputStream.write(chunk);
    outputStream.flush();
  }
}
```

- 응답 본문에 스트림 음성 답변(byte[ ])이 출력되므로 produces에 MediaType.APPLICATION_OCTET_STREAM_VALUE로 설정

```java
@PostMapping(
   value = "/chat-voice-stt-llm-tts", 
   consumes = MediaType.MULTIPART_FORM_DATA_VALUE, 
   produces = MediaType.APPLICATION_OCTET_STREAM_VALUE
 )
 public StreamingResponseBody chatVoiceSttLlmTts(
     @RequestParam("question") MultipartFile question,
     HttpServletResponse response) throws Exception {
   // 비동기 음성 데이터를 Flux<byte[]>을 얻기
   Flux<byte[]> flux = aiService.chatVoiceSttLlmTts(question.getBytes());

   // 음성 데이터를 응답 본문으로 스트림 출력
   StreamingResponseBody srd = new StreamingResponseBody() {
     @Override
     public void writeTo(OutputStream outputStream) throws IOException {
       for (byte[] chunk : flux.toIterable()) {
         outputStream.write(chunk);
         outputStream.flush();
       }
     }
   };    
   return srd;
 }
```

- HttpServlotRosponse를 이용해서 HTTP 응답 본문에 스트림 음성 답변을 출력합니다.
- Spring WebMVC에서는 StringHttpMessagConverter를 통해 텍스트를 스트림으로 출력함 수 있기 때문에 FIux〈String〉 을 반환 타입으로 사용할
  수 있습니다. 그러나 byte[]를 스트림으로 출력할 수 있는 HttpMessageConverter 는 제공되지 않기 때문에, FIux〈byte[ ]〉 를 반환 타입으로 사용할 수 없습니다.
- 그래서 HttpServletResponse 또는 StreamingResponseBody의 OutputStream을 이용해서 수동으로 출력해야 합니다.

## 순수 음성 대화 구현 2

Compare Models 활용하여 하나의 모델로 텍스트 응답과 음성 응답을 동시에 처리

AiService

```java
public byte[] chatVoiceOneModel(byte[] audioBytes, String mimeType) throws Exception {
    // 음성 데이터를 Resource로 생성
    Resource resource = new ByteArrayResource(audioBytes);

    // 사용자 메시지 생성
    UserMessage userMessage = UserMessage.builder()
        // 빈문자열이라도 제공해야함
        .text("제공되는 음성에 맞는 자연스러운 대화로 이어주세요.")
        .media(new Media(MimeType.valueOf(mimeType), resource))
        .build();

    // 모델 옵션 설정
    ChatOptions chatOptions = OpenAiChatOptions.builder()
        .model(OpenAiApi.ChatModel.GPT_4_O_MINI_AUDIO_PREVIEW)
        .outputModalities(List.of("text", "audio"))
        .outputAudio(new AudioParameters(
            ChatCompletionRequest.AudioParameters.Voice.ALLOY,
            ChatCompletionRequest.AudioParameters.AudioResponseFormat.MP3))
        .build();

    // gpt-4o-mini-audio 모델은 스트림을 지원하지 않기 때문에 동기 방식 사용
    // 모델로 요청하고 응답 받기
    ChatResponse response = chatClient.prompt()
        .system("50자 이내로 답변해주세요.")
        .messages(userMessage)
        .options(chatOptions)
        .call()
        .chatResponse();
    
    // AI 메시지 얻기
    AssistantMessage assistantMessage = response.getResult().getOutput();
    
    // 텍스트 답변 얻기
    String textAnswer = assistantMessage.getText();
    log.info("텍스트 응답: {}", textAnswer);

    // 오디오 답변 얻기
    byte[] audioAnswer = assistantMessage.getMedia().get(0).getDataAsByteArray();

    return audioAnswer;
  }
```

- chatVoiceSttLImTts() 메소드 선언부와 다른 점은 두번째 매개값으로 사용자 음성의
  오디오 MIME 타입을 받는다는 것입니다. 이 정보는 UserMessage에 포함되는Media 객체를 생성할 때 이용됩니다.
- UserMessage를 생성합니다. text()를 작성하지 않으면 에러가 나므로
  , 빈 문자열이라도 제공해야합니다. 사용자의 음성 질문은 Media 객체로
  생성해서 추가
- 모델 옵션을 설정합니다. model()에는 GPT_4_O_MINI_AUDIO_PREVIEW 열거 상수를 지정했습니다. 아직 정식 버전의 모델이 아니기 때문에 _PREVIEW가 붙어 있습니다. outpulModalities()에는 출력 형식을 지정해주어야 하는데, 텍스트 답변과 음성 답변 모두 출력하도록 했습니다. 그리고 outputAudio()에는 목소리의 종류 및
  촐력 오디오 포맷으로 MP3를 지정했습니다.
- 음성 모델로 요청하고 응답을 받습니다. gpt-40-mini-audio 모델은 스트리밍
  출력을 지원하지 않기 때문에, strearn () 이 아닌동기 방식인 call() 메소드로 호출해야합니다.
- ChatResponse로부터 AI 메시지인 AssistantMessage를 얻습니다.
- AssistantMessage로부터 텍스트 답변을 얻고, 로그로 출력합니다.
- AssistantMessage로부터 음성 답변(byte[])을 얻고 반환합니다.

AiController

```java
@PostMapping(
  value = "/chat-voice-one-model", 
  consumes = MediaType.MULTIPART_FORM_DATA_VALUE, 
  produces = MediaType.APPLICATION_OCTET_STREAM_VALUE
)
public byte[] chatVoiceOneModel(
  @RequestParam("question") MultipartFile question,
  HttpServletResponse response) throws Exception {
  byte[] bytes = aiService.chatVoiceOneModel(question.getBytes(), question.getContentType());
  return bytes;
}
```

- byte[]를 반환 타입으로 선언했습니다. 이렇게 하면 응답본문에 바로 음성 답변(byte[])을 출력할 수 있습니다.
- AiService의 chatVoiceOneModel() 메소드를 호출할 때 사용자의 음성 질문(byte[])과 오디오 타입을 매개값으로 제공합니다. 그리고 음성 답변(byte[])을 얻고, 반환합니다.