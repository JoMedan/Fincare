package Fincare.FincareAppProject.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

public class ChatbotRequestDTO {
    private String message;

    // 기본 생성자
    public ChatbotRequestDTO() {}

    // 생성자
    public ChatbotRequestDTO(String message) {
        this.message = message;
    }

    // Getter & Setter
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}


