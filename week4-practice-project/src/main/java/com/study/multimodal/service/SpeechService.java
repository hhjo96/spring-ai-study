package com.study.multimodal.service;

import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.OpenAiAudioSpeechOptions;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SpeechService {

    private final OpenAiAudioSpeechModel speechModel;
    private final OpenAiAudioTranscriptionModel transcriptionModel;
    private final ChatClient chatClient;

    public SpeechService(OpenAiAudioSpeechModel speechModel,
                       OpenAiAudioTranscriptionModel transcriptionModel,
                       ChatClient chatClient) {
        this.speechModel = speechModel;
        this.transcriptionModel = transcriptionModel;
        this.chatClient = chatClient;
    }

    public byte[] textToSpeech(String text, String voice) {
        OpenAiAudioSpeechOptions speechOptions = OpenAiAudioSpeechOptions.builder()
                .voice(voice != null ? voice : "nova")
                .speed(1.0)
                .responseFormat(OpenAiAudioApi.SpeechRequest.AudioResponseFormat.MP3)
                .build();

        TextToSpeechPrompt speechPrompt = new TextToSpeechPrompt(text, speechOptions);
        return speechModel.call(speechPrompt).getResult().getOutput();
    }

    public String speechToText(MultipartFile file) {
        OpenAiAudioTranscriptionOptions transcriptionOptions = OpenAiAudioTranscriptionOptions.builder()
                .language("ko")
                .build();

        return transcriptionModel.call(
                new AudioTranscriptionPrompt(file.getResource(), transcriptionOptions))
                .getResult()
                .getOutput();
    }

    public byte[] voiceAssistant(String question) {
        String response = chatClient
                .prompt()
                .user(question)
                .call()
                .content();

        return textToSpeech(response, "nova");
    }
}
