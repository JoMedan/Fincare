package Fincare.FincareAppProject.Controller;

import Fincare.FincareAppProject.Config.JwtUtil;
import Fincare.FincareAppProject.DTO.ChangePasswordDTO;
import Fincare.FincareAppProject.DTO.LoginRequestDTO;
import Fincare.FincareAppProject.DTO.LoginResponseDTO;
import Fincare.FincareAppProject.DTO.UserRegisterDTO;
import Fincare.FincareAppProject.Service.TokenService;
import Fincare.FincareAppProject.Service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final TokenService tokenService;

    public AuthController(UserService userService, JwtUtil jwtUtil, TokenService tokenService) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.tokenService = tokenService;
    }

    @Operation(summary = "회원가입", description = "새로운 사용자를 등록합니다.")
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody UserRegisterDTO userRegisterDTO) {
        String result = userService.register(userRegisterDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", result));
    }

    @Operation(summary = "로그인", description = "Access Token과 Refresh Token을 반환합니다.")
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequestDTO loginRequestDTO) {
        String username = loginRequestDTO.getUsername();
        String accessToken = userService.login(username, loginRequestDTO.getPassword());
        String refreshToken = jwtUtil.generateRefreshToken(username);
        tokenService.storeRefreshToken(username, refreshToken);
        return ResponseEntity.ok(new LoginResponseDTO(accessToken, refreshToken));
    }

    @Operation(summary = "토큰 갱신", description = "Refresh Token으로 새 Access Token을 발급합니다.")
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refresh(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        if (refreshToken == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "refreshToken이 필요합니다."));
        }

        String username;
        try {
            username = jwtUtil.validateTokenAndExtractUsername(refreshToken);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "유효하지 않은 Refresh Token입니다."));
        }

        if (!tokenService.isRefreshTokenValid(username, refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "만료되었거나 이미 사용된 Refresh Token입니다."));
        }

        String newAccessToken = jwtUtil.generateToken(username);
        return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
    }

    @Operation(summary = "로그아웃", description = "Access Token을 블랙리스트에 추가하고 Refresh Token을 삭제합니다.")
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @AuthenticationPrincipal String username,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            long remainingMs = jwtUtil.getRemainingExpiration(token);
            tokenService.blacklistAccessToken(token, remainingMs);
            if (username != null) {
                tokenService.deleteRefreshToken(username);
            }
            return ResponseEntity.ok(Map.of("message", "로그아웃 성공"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "인증되지 않은 사용자입니다."));
        }
    }

    @Operation(summary = "토큰 유효성 검사", description = "JWT Access Token의 유효성을 검사합니다.")
    @PostMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            jwtUtil.validateTokenAndExtractUsername(token);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @Operation(summary = "비밀번호 변경", description = "현재 비밀번호를 확인한 후 새 비밀번호로 변경합니다.")
    @PatchMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @AuthenticationPrincipal String username,
            @RequestBody ChangePasswordDTO passwordRequest) {
        userService.changePassword(username, passwordRequest.getCurrentPassword(), passwordRequest.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "비밀번호 변경 성공"));
    }

    @Operation(summary = "회원 탈퇴", description = "현재 비밀번호를 확인한 후 계정을 삭제합니다.")
    @DeleteMapping("/delete-account")
    public ResponseEntity<Map<String, String>> deleteAccount(
            @AuthenticationPrincipal String username,
            @RequestBody Map<String, String> deleteRequest) {
        userService.deleteAccount(username, deleteRequest.get("password"));
        return ResponseEntity.ok(Map.of("message", "회원 탈퇴 성공"));
    }
}
