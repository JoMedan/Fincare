package Fincare.FincareAppProject.Controller;

import Fincare.FincareAppProject.DTO.UpdateBudgetDTO;
import Fincare.FincareAppProject.DTO.UserResponseDTO;
import Fincare.FincareAppProject.Service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "내 정보 조회", description = "로그인한 사용자의 정보와 오늘 예산 현황을 조회합니다.")
    @GetMapping("/me")
    public UserResponseDTO getMyInfo(@AuthenticationPrincipal String username) {
        return userService.getUserInfo(username);
    }

    @Operation(summary = "예산 정보 조회", description = "월 수입 및 고정 지출 정보를 조회합니다.")
    @GetMapping("/budget-info")
    public Map<String, Object> getBudgetInfo(@AuthenticationPrincipal String username) {
        return userService.getBudgetInfo(username);
    }

    @Operation(summary = "세이프박스 초기화", description = "세이프박스 잔액을 0으로 초기화합니다.")
    @PatchMapping("/safe-box/reset")
    public ResponseEntity<Map<String, String>> resetSafeBox(@AuthenticationPrincipal String username) {
        userService.resetSafeBox(username);
        return ResponseEntity.ok(Map.of("message", "세이프박스가 초기화되었습니다."));
    }

    @Operation(summary = "월 예산 수정", description = "월 총 수입 및 고정 지출을 수정하고 하루 예산을 재계산합니다.")
    @PatchMapping("/budget")
    public ResponseEntity<Map<String, String>> updateBudget(
            @AuthenticationPrincipal String username,
            @RequestBody UpdateBudgetDTO updateBudgetDTO
    ) {
        userService.updateBudget(username, updateBudgetDTO);
        return ResponseEntity.ok(Map.of("message", "월 예산이 수정되었습니다."));
    }
}
