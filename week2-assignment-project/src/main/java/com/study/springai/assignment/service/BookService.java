package com.study.springai.assignment.service;

import com.study.springai.assignment.dto.BookAnalysisRequest;
import com.study.springai.assignment.dto.BookRecommendRequest;
import com.study.springai.assignment.dto.BookRecommendation;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class BookService {
    private final ChatClient chatClient;
    String systemPrompt = "너는 한국어로만 대답하는 AI야. 모든 설명은 반드시 한국어로만 대답해 줘. 영어 문장은 절대 쓰지 마.";

    public BookService(ChatClient chatClient) {

        this.chatClient = chatClient;
    }

    // TODO 1: 도서 추천 기능 구현 (Ch2 - 텍스트 대화)
    // ChatClient의 Fluent API(prompt → user → call → content)를 사용하여
    // request의 genre, mood, count 정보를 포함한 프롬프트를 작성하고 응답을 반환하세요.
    public String recommendBooks(BookRecommendRequest request) {
        String prompt = systemPrompt + String.format("%s 장르의 %s 분위기 책을 %d권 추천해줘. 각 책의 제목, 저자, 간단한 소개를 적어줘."
                ,request.genre(), request.mood(), request.count());
        //prompt: 프롬프트 만들기 시작
        // prompt: 질문
        // user: 사용자 메시지 설정
        // call: LLM 호출
        // content: 응답 텍스트 추출
        return chatClient.prompt().user(prompt).call().content();
    }

    // TODO 2: 도서 분석 기능 구현 (Ch3 - 프롬프트 템플릿)
    // prompts/book-analysis.st 템플릿 파일을 ClassPathResource로 로드한 뒤
    // PromptTemplate을 사용하여 변수를 치환하고 ChatClient로 실행하세요.
    public String analyzeBook(BookAnalysisRequest request) {

        //var 타입 자동결정(컴파일러가 추론)
        var template = new PromptTemplate(new ClassPathResource("prompts/book-analysis.st"));

        var prompt =  systemPrompt + template.create(Map.of(
                "title", request.title(),
                "author", request.author()
        ));

        return chatClient.prompt(prompt).call().content();
    }

    // TODO 3: 구조화된 도서 추천 기능 구현 (Ch4 - 구조화된 출력)
    // ChatClient의 entity() 메서드와 ParameterizedTypeReference를 사용하여
    // 응답을 List<BookRecommendation> 형태로 변환하여 반환하세요.
    public List<BookRecommendation> getStructuredRecommendations(BookRecommendRequest request) {
        String prompt =  systemPrompt + String.format(
                "%s 장르의 %s 분위기 책을 %d권 추천해줘.",
                request.genre(), request.mood(), request.count()
        );
        // entity 를 사용할 경우 자동으로 자바 객체로 매핑해줌
        return chatClient
                .prompt()
                .user(prompt)
                .call()
                .entity(new ParameterizedTypeReference<List<BookRecommendation>>() {});

    }

    // TODO 4: 제로-샷 도서 분류 기능 구현 (프롬프트 엔지니어링)
    // 예시를 제공하지 않고, 명확한 지시문만으로 도서 설명의 장르를 분류하세요.
    // 분류 카테고리와 분류 기준을 프롬프트에 명시하는 것이 핵심입니다.
    public String classifyBookZeroShot(String bookDescription) {
        String prompt = systemPrompt + """
            너는 도서 분류 전문가야.
            아래 도서 설명을 읽고, 가장 적합한 장르를 딱 하나만 선택해.
            
            [장르]
            - 소설 (Fiction)
            - 자기계발 (Self-Help)
            - 과학/기술 (Science/Tech)
            - 역사/사회 (History/Society)
            - 철학/심리 (Philosophy/Psychology)
            - 경제/경영 (Business/Economics)
            - 판타지/SF (Fantasy/SF)
            
            [분류 기준]
            - 책의 핵심 내용과 목적을 파악
            - 가장 지배적인 주제를 기준으로 할 것
            - 반드시 위 카테고리 중 하나만 선택하고, 이유를 한 문장으로 설명할 수 있어야 함
            
            [도서 설명]
            %s
            
            [출력 형식]
            장르: (선택한 카테고리)
            이유: (한 문장 설명)
            """.formatted(bookDescription);

        return chatClient
                .prompt()
                .user(prompt)
                .call()
                .content();
    }

    // TODO 5: 스텝-백 도서 분석 기능 구현 (프롬프트 엔지니어링)
    // 구체적 질문에 바로 답하지 않고, 먼저 상위 개념(배경, 원리)을 탐색한 뒤
    // 이를 바탕으로 답변을 도출하는 단계적 프롬프트를 설계하세요.
    public String analyzeWithStepBack(String title, String question) {
        String stepBackPrompt = systemPrompt + String.format("""
            '%s'라는 책과 관련된 질문에 답하기 전에,
            먼저 이 질문을 이해하는 데 필요한 배경 지식과 핵심 원리를 생각해.
            
            [질문]
            %s
            
            [1단계] 이 질문과 관련된 상위 개념, 철학적 배경, 또는 핵심 원리는 어떤 게 있는지?
            [2단계] 그 배경 지식을 바탕으로, '%s'의 맥락에서 질문에 쉽고 자세하게 대답하기
            """, title, question, title);

        return chatClient
                .prompt()
                .user(stepBackPrompt)
                .call()
                .content();
    }
}
