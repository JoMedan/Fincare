package Fincare.FincareAppProject.Service;

import Fincare.FincareAppProject.DTO.TransactionDTO;
import Fincare.FincareAppProject.Entity.Transaction;
import Fincare.FincareAppProject.Entity.User;
import Fincare.FincareAppProject.Repository.TransactionRepository;
import Fincare.FincareAppProject.Repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private TransactionService transactionService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setMonth_TotalIncome(3000000.0);
        testUser.setMonth_FixedExpense(1000000.0);
        testUser.setCurrentDailyBudget(50000.0);
        testUser.setSafeBox(100000.0);
        testUser.setLastUpdatedDate(LocalDate.now());
    }

    private TransactionDTO expenseDto(double amount) {
        TransactionDTO dto = new TransactionDTO(null, "지출", "식비", amount, LocalDate.now());
        return dto;
    }

    private TransactionDTO incomeDto(double amount) {
        return new TransactionDTO(null, "수입", "급여", amount, LocalDate.now());
    }

    // ──────────────────────────────────────────
    // createTransaction (세이프박스 사용)
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("거래 등록 - 세이프박스 사용")
    class CreateTransactionWithSafeBox {

        @Test
        @DisplayName("지출이 하루 예산 이내이면 하루 예산에서 차감한다")
        void expense_withinDailyBudget() {
            given(userRepository.findByUsername("testuser")).willReturn(Optional.of(testUser));

            transactionService.createTransaction("testuser", expenseDto(30000.0));

            assertThat(testUser.getCurrentDailyBudget()).isEqualTo(20000.0);
            verify(userRepository).save(testUser);
            verify(transactionRepository).save(any(Transaction.class));
        }

        @Test
        @DisplayName("지출이 하루 예산 초과 시 세이프박스에서 부족분을 차감하고 하루 예산을 0으로 설정한다")
        void expense_exceedsBudget_useSafeBox() {
            given(userRepository.findByUsername("testuser")).willReturn(Optional.of(testUser));

            transactionService.createTransaction("testuser", expenseDto(80000.0));

            // 50000 초과분 30000을 세이프박스에서 차감
            assertThat(testUser.getCurrentDailyBudget()).isZero();
            assertThat(testUser.getSafeBox()).isEqualTo(70000.0);
        }

        @Test
        @DisplayName("세이프박스도 부족하면 예외를 던진다")
        void expense_insufficientSafeBox() {
            testUser.setSafeBox(10000.0); // 초과분 30000보다 적음
            given(userRepository.findByUsername("testuser")).willReturn(Optional.of(testUser));

            assertThatThrownBy(() -> transactionService.createTransaction("testuser", expenseDto(80000.0)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("SafeBox 잔액이 부족합니다.");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("수입 등록 시 하루 예산에 더한다")
        void income_addsToDailyBudget() {
            given(userRepository.findByUsername("testuser")).willReturn(Optional.of(testUser));

            transactionService.createTransaction("testuser", incomeDto(20000.0));

            assertThat(testUser.getCurrentDailyBudget()).isEqualTo(70000.0);
        }
    }

    // ──────────────────────────────────────────
    // createTransactionWithoutSafeBox
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("거래 등록 - 세이프박스 미사용")
    class CreateTransactionWithoutSafeBox {

        @Test
        @DisplayName("지출이 예산 초과여도 세이프박스 건드리지 않고 하루 예산을 음수로 만든다")
        void expense_exceedsBudget_noSafeBox() {
            given(userRepository.findByUsername("testuser")).willReturn(Optional.of(testUser));

            transactionService.createTransactionWithoutSafeBox("testuser", expenseDto(80000.0));

            assertThat(testUser.getCurrentDailyBudget()).isEqualTo(-30000.0);
            assertThat(testUser.getSafeBox()).isEqualTo(100000.0); // 세이프박스 불변
        }
    }

    // ──────────────────────────────────────────
    // deleteTransaction
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("거래 삭제")
    class DeleteTransaction {

        @Test
        @DisplayName("지출 삭제 시 해당 금액을 하루 예산에 복구한다")
        void delete_expense_restoresDailyBudget() {
            Transaction transaction = new Transaction();
            transaction.setType("지출");
            transaction.setAmount(20000.0);
            transaction.setUser(testUser);

            given(transactionRepository.findById(1L)).willReturn(Optional.of(transaction));

            transactionService.deleteTransaction("testuser", 1L);

            assertThat(testUser.getCurrentDailyBudget()).isEqualTo(70000.0);
            verify(transactionRepository).delete(transaction);
        }

        @Test
        @DisplayName("수입 삭제 시 하루 예산은 변하지 않는다")
        void delete_income_noChange() {
            Transaction transaction = new Transaction();
            transaction.setType("수입");
            transaction.setAmount(20000.0);
            transaction.setUser(testUser);

            given(transactionRepository.findById(1L)).willReturn(Optional.of(transaction));

            transactionService.deleteTransaction("testuser", 1L);

            assertThat(testUser.getCurrentDailyBudget()).isEqualTo(50000.0);
        }

        @Test
        @DisplayName("다른 사용자의 거래는 삭제할 수 없다")
        void delete_otherUsersTransaction_throwsException() {
            User anotherUser = new User();
            anotherUser.setUsername("another");

            Transaction transaction = new Transaction();
            transaction.setType("지출");
            transaction.setAmount(10000.0);
            transaction.setUser(anotherUser);

            given(transactionRepository.findById(1L)).willReturn(Optional.of(transaction));

            assertThatThrownBy(() -> transactionService.deleteTransaction("testuser", 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("사용자가 소유한 거래 내역이 아닙니다.");

            verify(transactionRepository, never()).delete(any());
        }

        @Test
        @DisplayName("존재하지 않는 거래 삭제 시 예외를 던진다")
        void delete_notFound_throwsException() {
            given(transactionRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> transactionService.deleteTransaction("testuser", 999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("해당 거래 내역을 찾을 수 없습니다.");
        }
    }

    // ──────────────────────────────────────────
    // getDailyTotals
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("일별 수입/지출 조회")
    class GetDailyTotals {

        @Test
        @DisplayName("오늘 거래 내역의 지출/수입 합계와 예산 정보를 반환한다")
        void getDailyTotals_returnsCorrectTotals() {
            LocalDate today = LocalDate.now();

            Transaction expense = new Transaction();
            expense.setType("지출");
            expense.setAmount(20000.0);
            expense.setCategory("식비");
            expense.setDate(today);
            expense.setUser(testUser);

            Transaction income = new Transaction();
            income.setType("수입");
            income.setAmount(10000.0);
            income.setCategory("기타");
            income.setDate(today);
            income.setUser(testUser);

            given(transactionRepository.findByUserAndDate("testuser", today))
                    .willReturn(List.of(expense, income));
            given(userRepository.findByUsername("testuser")).willReturn(Optional.of(testUser));

            Map<String, Object> result = transactionService.getDailyTotals("testuser", today);

            assertThat(result.get("daily_expense_total")).isEqualTo(20000.0);
            assertThat(result.get("daily_income_total")).isEqualTo(10000.0);
            assertThat(result.get("current_daily_budget")).isEqualTo(50000.0);
            assertThat(result).containsKey("daily_budget_no_change");
            assertThat(result).containsKey("transactions");
        }
    }

    // ──────────────────────────────────────────
    // getMonthlySummary
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("월별 요약 조회")
    class GetMonthlySummary {

        @Test
        @DisplayName("이번달 총 지출, 총 수입, 카테고리별 지출을 반환한다")
        void getMonthlySummary_returnsCorrectData() {
            LocalDate today = LocalDate.now();

            Transaction t1 = new Transaction();
            t1.setType("지출");
            t1.setAmount(15000.0);
            t1.setCategory("식비");

            Transaction t2 = new Transaction();
            t2.setType("지출");
            t2.setAmount(10000.0);
            t2.setCategory("교통");

            Transaction t3 = new Transaction();
            t3.setType("수입");
            t3.setAmount(3000000.0);
            t3.setCategory("급여");

            given(transactionRepository.findByUserAndDateRange(
                    eq("testuser"), any(LocalDate.class), any(LocalDate.class)))
                    .willReturn(List.of(t1, t2, t3));

            Map<String, Object> result = transactionService.getMonthlySummary("testuser", today);

            assertThat(result.get("monthly_expense_total")).isEqualTo(25000.0);
            assertThat(result.get("monthly_income_total")).isEqualTo(3000000.0);

            @SuppressWarnings("unchecked")
            Map<String, Double> categoryExpenses = (Map<String, Double>) result.get("category_expenses");
            assertThat(categoryExpenses.get("식비")).isEqualTo(15000.0);
            assertThat(categoryExpenses.get("교통")).isEqualTo(10000.0);
        }
    }

    // ──────────────────────────────────────────
    // getDailyAdjustedBudget
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("하루 예산 조정 조회")
    class GetDailyAdjustedBudget {

        @Test
        @DisplayName("오늘 날짜가 lastUpdatedDate와 같으면 현재 하루 예산을 그대로 반환한다")
        void sameDate_returnsCurrentBudget() {
            testUser.setLastUpdatedDate(LocalDate.now());
            given(userRepository.findByUsername("testuser")).willReturn(Optional.of(testUser));

            double result = transactionService.getDailyAdjustedBudget("testuser", LocalDate.now());

            assertThat(result).isEqualTo(50000.0);
            verify(userRepository, never()).save(any()); // 저장 없음
        }

        @Test
        @DisplayName("날짜가 바뀌면 하루 예산을 초기값으로 리셋하고 저장한다")
        void newDate_resetsToInitialBudget() {
            testUser.setLastUpdatedDate(LocalDate.now().minusDays(1)); // 어제
            given(userRepository.findByUsername("testuser")).willReturn(Optional.of(testUser));

            double result = transactionService.getDailyAdjustedBudget("testuser", LocalDate.now());

            double expectedInitial = 2000000.0 / LocalDate.now().lengthOfMonth();
            assertThat(result).isEqualTo(expectedInitial);
            verify(userRepository).save(testUser);
        }
    }
}
