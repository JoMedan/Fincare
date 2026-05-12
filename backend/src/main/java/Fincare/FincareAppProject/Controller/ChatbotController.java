package Fincare.FincareAppProject.Controller;

import Fincare.FincareAppProject.DTO.ChatbotRequestDTO;
import Fincare.FincareAppProject.Service.OpenAiService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final OpenAiService openAiService;

    @Operation(summary = "챗봇 응답", description = "사용자 메시지를 AI에 전달하고 응답을 반환합니다.")
    @PostMapping
    public ResponseEntity<Map<String, String>> chatWithBot(@RequestBody ChatbotRequestDTO request) {
        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "message 값이 비어 있습니다."));
        }

        try {
            String botResponse = openAiService.getAIResponse(request.getMessage().trim());
            if (botResponse == null || botResponse.trim().isEmpty()) {
                botResponse = "현재 챗봇 응답을 받을 수 없습니다.";
            }
            return ResponseEntity.ok(Map.of("response", botResponse));
        } catch (Exception e) {
            log.error("챗봇 응답 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "챗봇 응답 중 오류 발생"));
        }
    }
}
