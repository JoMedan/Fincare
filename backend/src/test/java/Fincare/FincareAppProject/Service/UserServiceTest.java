package Fincare.FincareAppProject.Service;

import Fincare.FincareAppProject.Config.JwtUtil;
import Fincare.FincareAppProject.DTO.UpdateBudgetDTO;
import Fincare.FincareAppProject.DTO.UserRegisterDTO;
import Fincare.FincareAppProject.DTO.UserResponseDTO;
import Fincare.FincareAppProject.Entity.User;
import Fincare.FincareAppProject.Exception.InvalidPasswordException;
import Fincare.FincareAppProject.Exception.UserNotFoundException;
import Fincare.FincareAppProject.Repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private JwtUtil jwtUtil;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TransactionService transactionService;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPassword("encodedPassword");
        testUser.setName("홍길동");
        testUser.setBirthDate("1995-01-01");
        testUser.setMonth_TotalIncome(3000000.0);
        testUser.setMonth_FixedExpense(1000000.0);
        testUser.setCurrentDailyBudget(66666.0);
        testUser.setSafeBox(0.0);
        testUser.setLastUpdatedDate(LocalDate.now());
    }

    // ──────────────────────────────────────────
    // register
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("회원가입")
    class Register {

        @Test
        @DisplayName("정상 입력이면 사용자를 저장하고 성공 메시지를 반환한다")
        void register_success() {
            UserRegisterDTO dto = new UserRegisterDTO();
            dto.setUsername("newuser");
            dto.setPassword("password123");
            dto.setName("홍길동");
            dto.setBirthDate("1995-01-01");
            dto.setMonth_TotalIncome(3000000.0);
            dto.setMonth_FixedExpense(1000000.0);

            given(passwordEncoder.encode(anyString())).willReturn("encodedPassword");

            String result = userService.register(dto);

            assertThat(result).isEqualTo("User registered successfully");
            verify(userRepository, times(1)).save(any(User.class));
        }

        @Test
        @DisplayName("회원가입 시 하루 예산은 (월수입 - 고정지출) / 당월일수로 계산된다")
        void register_calculatesInitialDailyBudget() {
            UserRegisterDTO dto = new UserRegisterDTO();
            dto.setUsername("newuser");
            dto.setPassword("pw");
            dto.setName("홍길동");
            dto.setBirthDate("1995-01-01");
            dto.setMonth_TotalIncome(3100000.0);
            dto.setMonth_FixedExpense(100000.0);

            given(passwordEncoder.encode(anyString())).willReturn("encodedPw");

            userService.register(dto);

            verify(userRepository).save(argThat(user -> {
                double expected = 3000000.0 / LocalDate.now().lengthOfMonth();
                return Math.abs(user.getCurrentDailyBudget() - expected) < 0.01;
            }));
        }
    }

    // ──────────────────────────────────────────
    // login
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("로그인")
    class Login {

        @Test
        @DisplayName("올바른 자격증명이면 JWT 토큰을 반환한다")
        void login_success() {
            given(userRepository.findByUsername("testuser")).willReturn(Optional.of(testUser));
            given(passwordEncoder.matches("password123", "encodedPassword")).willReturn(true);
            given(jwtUtil.generateToken("testuser")).willReturn("jwt-token");

            String token = userService.login("testuser", "password123");

            assertThat(token).isEqualTo("jwt-token");
        }

        @Test
        @DisplayName("존재하지 않는 사용자면 UserNotFoundException을 던진다")
        void login_userNotFound() {
            given(userRepository.findByUsername("unknown")).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.login("unknown", "pw"))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("비밀번호가 틀리면 InvalidPasswordException을 던진다")
        void login_wrongPassword() {
            given(userRepository.findByUsername("testuser")).willReturn(Optional.of(testUser));
            given(passwordEncoder.matches("wrongPw", "encodedPassword")).willReturn(false);

            assertThatThrownBy(() -> userService.login("testuser", "wrongPw"))
                    .isInstanceOf(InvalidPasswordException.class)
                    .hasMessage("비밀번호가 일치하지 않습니다.");
        }
    }

    // ──────────────────────────────────────────
    // changePassword
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("비밀번호 변경")
    class ChangePassword {

        @Test
        @DisplayName("현재 비밀번호가 맞으면 새 비밀번호로 변경한다")
        void changePassword_success() {
            given(userRepository.findByUsername("testuser")).willReturn(Optional.of(testUser));
            given(passwordEncoder.matches("currentPw", "encodedPassword")).willReturn(true);
            given(passwordEncoder.encode("newPw")).willReturn("newEncodedPw");

            userService.changePassword("testuser", "currentPw", "newPw");

            assertThat(testUser.getPassword()).isEqualTo("newEncodedPw");
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("현재 비밀번호가 틀리면 InvalidPasswordException을 던진다")
        void changePassword_wrongCurrentPassword() {
            given(userRepository.findByUsername("testuser")).willReturn(Optional.of(testUser));
            given(passwordEncoder.matches("wrongPw", "encodedPassword")).willReturn(false);

            assertThatThrownBy(() -> userService.changePassword("testuser", "wrongPw", "newPw"))
                    .isInstanceOf(InvalidPasswordException.class);

            verify(userRepository, never()).save(any());
        }
    }

    // ──────────────────────────────────────────
    // deleteAccount
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("회원 탈퇴")
    class DeleteAccount {

        @Test
        @DisplayName("비밀번호가 맞으면 계정을 삭제한다")
        void deleteAccount_success() {
            given(userRepository.findByUsername("testuser")).willReturn(Optional.of(testUser));
            given(passwordEncoder.matches("password123", "encodedPassword")).willReturn(true);

            userService.deleteAccount("testuser", "password123");

            verify(userRepository).delete(testUser);
        }

        @Test
        @DisplayName("비밀번호가 틀리면 InvalidPasswordException을 던지고 삭제하지 않는다")
        void deleteAccount_wrongPassword() {
            given(userRepository.findByUsername("testuser")).willReturn(Optional.of(testUser));
            given(passwordEncoder.matches("wrongPw", "encodedPassword")).willReturn(false);

            assertThatThrownBy(() -> userService.deleteAccount("testuser", "wrongPw"))
                    .isInstanceOf(InvalidPasswordException.class);

            verify(userRepository, never()).delete(any());
        }
    }

    // ──────────────────────────────────────────
    // resetSafeBox
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("세이프박스 초기화")
    class ResetSafeBox {

        @Test
        @DisplayName("세이프박스 잔액을 0으로 초기화한다")
        void resetSafeBox_success() {
            testUser.setSafeBox(50000.0);
            given(userRepository.findByUsername("testuser")).willReturn(Optional.of(testUser));

            userService.resetSafeBox("testuser");

            assertThat(testUser.getSafeBox()).isZero();
            verify(userRepository).save(testUser);
        }
    }

    // ──────────────────────────────────────────
    // updateBudget
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("월 예산 수정")
    class UpdateBudget {

        @Test
        @DisplayName("월 수입과 고정지출을 수정하면 하루 예산이 재계산된다")
        void updateBudget_recalculatesDailyBudget() {
            given(userRepository.findByUsername("testuser")).willReturn(Optional.of(testUser));

            UpdateBudgetDTO dto = new UpdateBudgetDTO();
            dto.setMonthTotalIncome(4000000.0);
            dto.setMonthFixedExpense(1000000.0);

            userService.updateBudget("testuser", dto);

            double expectedDaily = 3000000.0 / LocalDate.now().lengthOfMonth();
            assertThat(testUser.getCurrentDailyBudget()).isEqualTo(expectedDaily);
            verify(userRepository).save(testUser);
        }
    }
}
