package Fincare.FincareAppProject.Controller;

import Fincare.FincareAppProject.DTO.ChangePasswordDTO;
import Fincare.FincareAppProject.DTO.LoginRequestDTO;
import Fincare.FincareAppProject.DTO.UserRegisterDTO;
import Fincare.FincareAppProject.Entity.User;
import Fincare.FincareAppProject.Config.JwtUtil;
import Fincare.FincareAppProject.Service.UserService;
import Fincare.FincareAppProject.Repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil; // JWT 관리
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    private static final Map<String, Boolean> tokenBlacklist = new ConcurrentHashMap<>(); // JWT 블랙리스트 관리

    public AuthController(UserService userService, JwtUtil jwtUtil, PasswordEncoder passwordEncoder, UserRepository userRepository) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    // 🔹 회원가입
    @Operation(summary = "회원가입", description = "새로운 사용자를 등록합니다.")
    @PostMapping("/register")
    public String register(@Valid @RequestBody UserRegisterDTO userRegisterDTO) {
        return userService.register(userRegisterDTO);
    }

    // 🔹 로그인
    @Operation(summary = "로그인", description = "사용자 이름과 비밀번호로 로그인합니다.")
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody LoginRequestDTO loginRequestDTO) {
        String token = userService.login(loginRequestDTO.getUsername(), loginRequestDTO.getPassword());

        // JSON 형식으로 반환
        Map<String, String> response = new HashMap<>();
        response.put("token", token);

        return ResponseEntity.ok(response);
    }

    // 🔹 JWT 토큰 유효성 검사
    @Operation(summary = "토큰 유효성 검사", description = "JWT 토큰의 유효성을 검사합니다.")
    @PostMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            // 'Bearer ' 제거 후 토큰 추출
            String token = authHeader.replace("Bearer ", "");
            jwtUtil.validateTokenAndExtractUsername(token); // 토큰 검증
            return ResponseEntity.ok().build(); // 유효하면 200 OK
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // 유효하지 않으면 401
        }
    }

    // 🔹 비밀번호 변경 API
    @PatchMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @AuthenticationPrincipal String username,
            @RequestBody ChangePasswordDTO passwordRequest) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String currentPassword = passwordRequest.getCurrentPassword();
        String newPassword = passwordRequest.getNewPassword();

        // 현재 비밀번호 검증
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "현재 비밀번호가 일치하지 않습니다."));
        }

        // 비밀번호 변경 후 저장
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "비밀번호 변경 성공"));
    }

    // 🔹 회원 탈퇴 API
    @Operation(summary = "회원 탈퇴", description = "현재 비밀번호를 확인한 후, 계정을 삭제합니다.")
    @DeleteMapping("/delete-account")
    public ResponseEntity<Map<String, String>> deleteAccount(
            @AuthenticationPrincipal String username,
            @RequestBody Map<String, String> deleteRequest) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String password = deleteRequest.get("password");

        // 현재 비밀번호 검증
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "비밀번호가 일치하지 않습니다."));
        }

        // 사용자 계정 삭제
        userRepository.delete(user);

        return ResponseEntity.ok(Map.of("message", "회원 탈퇴 성공"));
    }

    // 🔹 로그아웃 API
    @Operation(summary = "로그아웃", description = "사용자의 JWT 토큰을 블랙리스트에 추가하여 로그아웃합니다.")
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            if (jwtUtil.validateToken(token)) {
                tokenBlacklist.put(token, true); // 블랙리스트에 추가
                return ResponseEntity.ok(Map.of("message", "로그아웃 성공"));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "유효하지 않은 토큰입니다."));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "인증되지 않은 사용자입니다."));
        }
    }

    // 🔹 블랙리스트 확인 메서드
    public static boolean isTokenBlacklisted(String token) {
        return tokenBlacklist.getOrDefault(token, false);
    }
}
