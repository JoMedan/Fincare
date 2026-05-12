package Fincare.FincareAppProject.Controller;

import Fincare.FincareAppProject.DTO.ChatbotRequestDTO;
import Fincare.FincareAppProject.Service.OpenAiService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/chatbot")
public class ChatbotController {

    private final OpenAiService openAiService;

    public ChatbotController(OpenAiService openAiService) {
        this.openAiService = openAiService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> chatWithBot(@RequestBody ChatbotRequestDTO request) {
        System.out.println("🚀 [ChatbotController] 챗봇 API 호출됨 - 메시지: " + request.getMessage());

        // ✅ Null 또는 빈 값 체크
        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "❌ message 값이 비어 있습니다."));
        }

        try {
            // OpenAI API 호출
            String botResponse = openAiService.getAIResponse(request.getMessage().trim());
            System.out.println("✅ [ChatbotController] OpenAI 응답: " + botResponse);

            // ✅ `null` 체크 및 기본 응답 제공
            if (botResponse == null || botResponse.trim().isEmpty()) {
                botResponse = "⚠️ 현재 챗봇 응답을 받을 수 없습니다.";
            }

            // ✅ HashMap 사용하여 `null` 예외 방지
            Map<String, String> response = new HashMap<>();
            response.put("response", botResponse);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ [ChatbotController] 오류 발생: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "챗봇 응답 중 오류 발생"));
        }
    }
}