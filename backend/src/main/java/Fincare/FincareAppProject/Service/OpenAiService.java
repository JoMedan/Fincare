package Fincare.FincareAppProject.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class OpenAiService {

    @Value("${openai.api.key}")
    private String apiKey; // OpenAI API 키

    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";

    public String getAIResponse(String userMessage) {
        RestTemplate restTemplate = new RestTemplate();

        // OpenAI API 요청 데이터 구성
        Map<String, Object> requestBody = Map.of(
                "model", "gpt-4",
                "messages", List.of(Map.of("role", "user", "content", userMessage)),
                "max_tokens", 500
        );

        // HTTP 요청 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(OPENAI_URL, HttpMethod.POST, requestEntity, Map.class);

            // ✅ 응답이 정상적으로 왔는지 확인
            if (response.getBody() == null || !response.getBody().containsKey("choices")) {
                System.err.println("❌ [OpenAiService] OpenAI 응답이 비어 있음");
                return "⚠️ 챗봇 응답을 받을 수 없습니다.";
            }

            // ✅ 응답에서 "choices" 확인 및 파싱
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");

            if (choices == null || choices.isEmpty() || !choices.get(0).containsKey("message")) {
                System.err.println("❌ [OpenAiService] OpenAI 응답에서 'message' 키를 찾을 수 없음");
                return "⚠️ 챗봇 응답을 받을 수 없습니다.";
            }

            // ✅ `choices[0].message.content`에서 안전하게 데이터 추출
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String botMessage = message != null ? (String) message.get("content") : "⚠️ 챗봇 응답을 받을 수 없습니다.";

            System.out.println("✅ [OpenAiService] 챗봇 응답: " + botMessage);
            return botMessage;

        } catch (Exception e) {
            System.err.println("❌ [OpenAiService] OpenAI API 요청 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            return "⚠️ OpenAI API 호출 중 오류 발생";
        }
    }
}
