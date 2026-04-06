package com.study.springai.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Random;

/**
 * 날씨 조회 도구 (더미 데이터 기반)
 * 실제 API 연동 없이 Tool Calling 흐름을 연습할 수 있습니다.
 */
@Slf4j
@Component
public class WeatherTools {

    private static final Map<String, String> WEATHER_DATA = Map.of(
            "서울", "맑음, 22도",
            "부산", "흐림, 19도",
            "대전", "비, 17도",
            "광주", "구름많음, 20도",
            "제주", "맑음, 24도",
            "인천", "안개, 18도",
            "대구", "맑음, 25도"
    );

    @Tool(description = "한 개 도시의 현재 날씨 정보를 조회합니다. 반드시 도시가 1개일 때만 사용하세요. 2개 도시 비교는 compareWeather를 사용하세요.")
    public String getWeather(
            @ToolParam(description = "날씨를 조회할 도시 이름 (예: 서울, 부산)", required = true)
            String city) {
        log.info("날씨 조회 요청 - 도시: {}", city);

        String weather = WEATHER_DATA.get(city);
        if (weather != null) {
            return "%s의 현재 날씨: %s".formatted(city, weather);
        }
        return "%s의 날씨 정보를 찾을 수 없습니다. 지원 도시: %s".formatted(city, String.join(", ", WEATHER_DATA.keySet()));
    }

    @Tool(description = "두 도시의 날씨를 비교합니다. 사용자가 두 개의 도시 날씨를 비교해달라고 요청하면 반드시 이 도구를 사용하세요. getWeather를 두 번 호출하지 말고 이 도구 하나로 처리하세요.")
    public String compareWeather(
            @ToolParam(description = "첫 번째 도시 이름", required = true) String city1,
            @ToolParam(description = "두 번째 도시 이름", required = true) String city2) {
        log.info("날씨 비교 요청 - {} vs {}", city1, city2);

        String weather1 = WEATHER_DATA.getOrDefault(city1, "정보 없음");
        String weather2 = WEATHER_DATA.getOrDefault(city2, "정보 없음");

        return "%s: %s | %s: %s".formatted(city1, weather1, city2, weather2);
    }
}

