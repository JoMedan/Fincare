package Fincare.FincareAppProject.Controller;

import Fincare.FincareAppProject.Config.JwtFilter;
import Fincare.FincareAppProject.Config.JwtUtil;
import Fincare.FincareAppProject.Exception.GlobalExceptionHandler;
import Fincare.FincareAppProject.Exception.InvalidPasswordException;
import Fincare.FincareAppProject.Service.TokenService;
import Fincare.FincareAppProject.Service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean UserService userService;
    @MockitoBean JwtUtil jwtUtil;
    @MockitoBean JwtFilter jwtFilter;
    @MockitoBean TokenService tokenService;

    // ──────────────────────────────────────────
    // POST /auth/register
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("POST /auth/register")
    class Register {

        @Test
        @DisplayName("정상 입력이면 201 Created와 성공 메시지를 반환한다")
        void register_success() throws Exception {
            given(userService.register(any())).willReturn("User registered successfully");

            Map<String, Object> body = Map.of(
                    "username", "testuser",
                    "password", "password123",
                    "name", "홍길동",
                    "birthDate", "1995-01-01",
                    "month_TotalIncome", 3000000,
                    "month_FixedExpense", 1000000
            );

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message").value("User registered successfully"));
        }

        @Test
        @DisplayName("필수 필드가 없으면 400 Bad Request를 반환한다")
        void register_missingField_returns400() throws Exception {
            Map<String, Object> body = Map.of("username", "testuser");

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ──────────────────────────────────────────
    // POST /auth/login
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("POST /auth/login")
    class Login {

        @Test
        @DisplayName("올바른 자격증명이면 200과 accessToken·refreshToken을 반환한다")
        void login_success() throws Exception {
            given(userService.login("testuser", "password123")).willReturn("mocked-access-token");
            given(jwtUtil.generateRefreshToken("testuser")).willReturn("mocked-refresh-token");
            willDoNothing().given(tokenService).storeRefreshToken(anyString(), anyString());

            Map<String, String> body = Map.of("username", "testuser", "password", "password123");

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("mocked-access-token"))
                    .andExpect(jsonPath("$.refreshToken").value("mocked-refresh-token"));
        }

        @Test
        @DisplayName("비밀번호가 틀리면 400 Bad Request를 반환한다")
        void login_wrongPassword_returns400() throws Exception {
            given(userService.login(anyString(), anyString()))
                    .willThrow(new InvalidPasswordException());

            Map<String, String> body = Map.of("username", "testuser", "password", "wrong");

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("비밀번호가 일치하지 않습니다."));
        }
    }

    // ──────────────────────────────────────────
    // POST /auth/refresh
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("POST /auth/refresh")
    class Refresh {

        @Test
        @DisplayName("유효한 Refresh Token이면 200과 새 accessToken을 반환한다")
        void refresh_success() throws Exception {
            given(jwtUtil.validateTokenAndExtractUsername("valid-refresh-token")).willReturn("testuser");
            given(tokenService.isRefreshTokenValid("testuser", "valid-refresh-token")).willReturn(true);
            given(jwtUtil.generateToken("testuser")).willReturn("new-access-token");

            Map<String, String> body = Map.of("refreshToken", "valid-refresh-token");

            mockMvc.perform(post("/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("new-access-token"));
        }

        @Test
        @DisplayName("Redis에 없는 Refresh Token이면 401을 반환한다")
        void refresh_invalidToken_returns401() throws Exception {
            given(jwtUtil.validateTokenAndExtractUsername("unknown-token")).willReturn("testuser");
            given(tokenService.isRefreshTokenValid("testuser", "unknown-token")).willReturn(false);

            Map<String, String> body = Map.of("refreshToken", "unknown-token");

            mockMvc.perform(post("/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ──────────────────────────────────────────
    // POST /auth/logout
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("POST /auth/logout")
    class Logout {

        @Test
        @DisplayName("유효한 토큰이면 200과 로그아웃 성공 메시지를 반환한다")
        void logout_success() throws Exception {
            given(jwtUtil.getRemainingExpiration("valid-token")).willReturn(3600000L);
            willDoNothing().given(tokenService).blacklistAccessToken(anyString(), anyLong());
            willDoNothing().given(tokenService).deleteRefreshToken(any());

            mockMvc.perform(post("/auth/logout")
                            .header("Authorization", "Bearer valid-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("로그아웃 성공"));
        }

        @Test
        @DisplayName("토큰이 없으면 401을 반환한다")
        void logout_noToken_returns401() throws Exception {
            mockMvc.perform(post("/auth/logout"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ──────────────────────────────────────────
    // PATCH /auth/change-password
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("PATCH /auth/change-password")
    class ChangePassword {

        @Test
        @DisplayName("올바른 현재 비밀번호이면 200과 성공 메시지를 반환한다")
        void changePassword_success() throws Exception {
            Map<String, String> body = Map.of(
                    "currentPassword", "currentPw",
                    "newPassword", "newPw"
            );

            mockMvc.perform(patch("/auth/change-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("비밀번호 변경 성공"));
        }

        @Test
        @DisplayName("현재 비밀번호가 틀리면 400을 반환한다")
        void changePassword_wrongCurrent_returns400() throws Exception {
            willThrow(new InvalidPasswordException())
                    .given(userService).changePassword(any(), anyString(), anyString());

            Map<String, String> body = Map.of(
                    "currentPassword", "wrong",
                    "newPassword", "newPw"
            );

            mockMvc.perform(patch("/auth/change-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("비밀번호가 일치하지 않습니다."));
        }
    }
}
