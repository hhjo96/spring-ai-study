package com.study.springai.service;

import com.study.springai.tool.BookingTools;
import com.study.springai.tool.DateTimeTools;
import com.study.springai.tool.WeatherTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class ToolCallingService {

    // ##### 필드 #####
    private final ChatClient chatClient;
    private final DateTimeTools dateTimeTools;
    private final WeatherTools weatherTools;
    private final BookingTools bookingTools;

    // ##### 생성자 #####
    public ToolCallingService(ChatModel chatModel, DateTimeTools dateTimeTools, WeatherTools weatherTools, BookingTools bookingTools) {
        this.chatClient = ChatClient.builder(chatModel).build();
        this.dateTimeTools = dateTimeTools;
        this.weatherTools = weatherTools;
        this.bookingTools = bookingTools;
    }

    // ##### 날짜/시간 도구를 사용한 대화 #####
    public String chatWithDateTime(String question) {
        String answer = this.chatClient.prompt()
                .user(question)
                .tools(dateTimeTools)
                .call()
                .content();
        return answer;
    }

    // ##### 날씨 도구를 사용한 대화 #####
    public String chatWithWeather(String question) {
        String answer = this.chatClient.prompt()
                .system("""
                    한국어로 친절하게 답변해주세요. 날씨 정보를 기반으로 옷차림이나 활동을 추천해주세요.
                    중요: 두 도시의 날씨를 비교하는 요청에는 반드시 compareWeather 도구를 사용하세요.
                    getWeather를 두 번 호출하지 마세요.
                """)
                .user(question)
                .tools(weatherTools)
                .call()
                .content();
        return answer;
    }

    // ##### 회의실 예약 도구를 사용한 대화 (ToolContext 활용) #####
    public String chatWithBooking(String question, String userName) {
        String answer = this.chatClient.prompt()
                .system("""
                    당신은 회의실 예약 도우미입니다.
                    사용자의 요청에 따라 회의실을 조회, 예약, 취소할 수 있습니다.
                    예약 시 반드시 회의실 이름, 날짜, 시간 정보가 필요합니다.
                    한국어로 친절하게 응대해주세요.
                """)
                .user(question)
                .tools(bookingTools)
                .toolContext(Map.of("userName", userName))
                .call()
                .content();
        return answer;
    }
}
