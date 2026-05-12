package Fincare.FincareAppProject.Service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
public class OpenAiService {

    @Value("${openai.api.key}")
    private String apiKey;

    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";

    public String getAIResponse(String userMessage) {
        RestTemplate restTemplate = new RestTemplate();

        Map<String, Object> requestBody = Map.of(
                "model", "gpt-4",
                "messages", List.of(Map.of("role", "user", "content", userMessage)),
                "max_tokens", 500
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    OPENAI_URL, HttpMethod.POST, new HttpEntity<>(requestBody, headers), Map.class);

            if (response.getBody() == null || !response.getBody().containsKey("choices")) {
                log.warn("OpenAI 응답이 비어 있음");
                return "챗봇 응답을 받을 수 없습니다.";
            }

            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
            if (choices == null || choices.isEmpty()) {
                log.warn("OpenAI 응답에서 choices를 찾을 수 없음");
                return "챗봇 응답을 받을 수 없습니다.";
            }

            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            return message != null ? (String) message.get("content") : "챗봇 응답을 받을 수 없습니다.";

        } catch (Exception e) {
            log.error("OpenAI API 호출 중 오류 발생", e);
            return "OpenAI API 호출 중 오류 발생";
        }
    }
}
