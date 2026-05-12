package Fincare.FincareAppProject.Service;

import Fincare.FincareAppProject.DTO.MonthlyTrendDTO;
import Fincare.FincareAppProject.Entity.Transaction;
import Fincare.FincareAppProject.Entity.User;
import Fincare.FincareAppProject.Enums.TransactionType;
import Fincare.FincareAppProject.Repository.TransactionRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AnalysisServiceTest {

    @Mock private TransactionRepository transactionRepository;

    @InjectMocks
    private AnalysisService analysisService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("testuser");
    }

    private Transaction tx(TransactionType type, String category, double amount, LocalDate date) {
        Transaction t = new Transaction();
        t.setUser(testUser);
        t.setType(type);
        t.setCategory(category);
        t.setAmount(amount);
        t.setDate(date);
        return t;
    }

    // ──────────────────────────────────────────
    // getMonthlyTrend
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("월별 트렌드")
    class MonthlyTrend {

        @Test
        @DisplayName("months=3이면 3개월치 DTO 리스트를 오래된 순으로 반환한다")
        void monthlyTrend_returnsCorrectMonthCount() {
            LocalDate today = LocalDate.now();
            LocalDate rangeStart = today.minusMonths(2).withDayOfMonth(1);

            given(transactionRepository.findByUserAndDateRange(eq("testuser"), eq(rangeStart), any()))
                    .willReturn(List.of());

            List<MonthlyTrendDTO> result = analysisService.getMonthlyTrend("testuser", 3);

            assertThat(result).hasSize(3);
            assertThat(result.get(2).getYear()).isEqualTo(today.getYear());
            assertThat(result.get(2).getMonth()).isEqualTo(today.getMonthValue());
        }

        @Test
        @DisplayName("해당 월 거래 내역의 수입·지출 합계를 올바르게 계산한다")
        void monthlyTrend_calculatesCorrectTotals() {
            LocalDate thisMonth = LocalDate.now().withDayOfMonth(1);

            List<Transaction> txns = List.of(
                    tx(TransactionType.INCOME, "급여", 3000000, thisMonth),
                    tx(TransactionType.EXPENSE, "식비", 50000, thisMonth),
                    tx(TransactionType.EXPENSE, "교통", 30000, thisMonth)
            );

            given(transactionRepository.findByUserAndDateRange(eq("testuser"), any(), any()))
                    .willReturn(txns);

            List<MonthlyTrendDTO> result = analysisService.getMonthlyTrend("testuser", 1);

            MonthlyTrendDTO current = result.get(0);
            assertThat(current.getIncomeTotal()).isEqualTo(3000000);
            assertThat(current.getExpenseTotal()).isEqualTo(80000);
            assertThat(current.getNetAmount()).isEqualTo(2920000);
        }

        @Test
        @DisplayName("months가 범위를 벗어나면 기본값 6으로 처리한다")
        void monthlyTrend_outOfRangeFallsBackToDefault() {
            given(transactionRepository.findByUserAndDateRange(any(), any(), any()))
                    .willReturn(List.of());

            List<MonthlyTrendDTO> result = analysisService.getMonthlyTrend("testuser", 100);
            assertThat(result).hasSize(6);
        }
    }

    // ──────────────────────────────────────────
    // getMonthlyComparison
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("전월 대비 증감률")
    class MonthlyComparison {

        @Test
        @DisplayName("이번 달과 전월의 지출 증감액·증감률을 반환한다")
        void comparison_calculatesExpenseChange() {
            LocalDate today = LocalDate.now();
            LocalDate thisStart = today.withDayOfMonth(1);
            LocalDate lastStart = today.minusMonths(1).withDayOfMonth(1);
            LocalDate lastEnd = lastStart.withDayOfMonth(lastStart.lengthOfMonth());

            given(transactionRepository.findByUserAndDateRange("testuser", thisStart, today))
                    .willReturn(List.of(tx(TransactionType.EXPENSE, "식비", 100000, thisStart)));
            given(transactionRepository.findByUserAndDateRange("testuser", lastStart, lastEnd))
                    .willReturn(List.of(tx(TransactionType.EXPENSE, "식비", 80000, lastStart)));

            Map<String, Object> result = analysisService.getMonthlyComparison("testuser");

            @SuppressWarnings("unchecked")
            Map<String, Object> expenseChange = (Map<String, Object>) result.get("expenseChange");
            assertThat((double) expenseChange.get("amount")).isEqualTo(20000.0);
            assertThat((double) expenseChange.get("rate")).isEqualTo(25.0);
        }

        @Test
        @DisplayName("전월 지출이 0이면 증감률을 100%로 반환한다")
        void comparison_previousZeroReturns100Rate() {
            LocalDate today = LocalDate.now();
            LocalDate thisStart = today.withDayOfMonth(1);
            LocalDate lastStart = today.minusMonths(1).withDayOfMonth(1);
            LocalDate lastEnd = lastStart.withDayOfMonth(lastStart.lengthOfMonth());

            given(transactionRepository.findByUserAndDateRange("testuser", thisStart, today))
                    .willReturn(List.of(tx(TransactionType.EXPENSE, "식비", 50000, thisStart)));
            given(transactionRepository.findByUserAndDateRange("testuser", lastStart, lastEnd))
                    .willReturn(List.of());

            Map<String, Object> result = analysisService.getMonthlyComparison("testuser");

            @SuppressWarnings("unchecked")
            Map<String, Object> expenseChange = (Map<String, Object>) result.get("expenseChange");
            assertThat((double) expenseChange.get("rate")).isEqualTo(100.0);
        }
    }

    // ──────────────────────────────────────────
    // getCategoryBreakdown
    // ──────────────────────────────────────────
    @Nested
    @DisplayName("카테고리별 지출 분석")
    class CategoryBreakdown {

        @Test
        @DisplayName("카테고리별 금액과 비율(%)을 올바르게 반환한다")
        void categoryBreakdown_calculatesAmountAndRate() {
            LocalDate start = LocalDate.of(2026, 4, 1);
            LocalDate end = LocalDate.of(2026, 4, 30);

            given(transactionRepository.findByUserAndDateRange("testuser", start, end))
                    .willReturn(List.of(
                            tx(TransactionType.EXPENSE, "식비", 60000, start),
                            tx(TransactionType.EXPENSE, "교통", 40000, start),
                            tx(TransactionType.INCOME, "급여", 3000000, start)
                    ));

            Map<String, Object> result = analysisService.getCategoryBreakdown("testuser", 2026, 4);

            assertThat(result.get("totalExpense")).isEqualTo(100000.0);

            @SuppressWarnings("unchecked")
            Map<String, Double> rates = (Map<String, Double>) result.get("categoryRates");
            assertThat(rates.get("식비")).isEqualTo(60.0);
            assertThat(rates.get("교통")).isEqualTo(40.0);
        }

        @Test
        @DisplayName("지출이 없으면 비율이 모두 0이다")
        void categoryBreakdown_noExpense_allZeroRates() {
            LocalDate start = LocalDate.of(2026, 4, 1);
            LocalDate end = LocalDate.of(2026, 4, 30);

            given(transactionRepository.findByUserAndDateRange("testuser", start, end))
                    .willReturn(List.of(tx(TransactionType.INCOME, "급여", 3000000, start)));

            Map<String, Object> result = analysisService.getCategoryBreakdown("testuser", 2026, 4);

            assertThat(result.get("totalExpense")).isEqualTo(0.0);

            @SuppressWarnings("unchecked")
            Map<String, Double> rates = (Map<String, Double>) result.get("categoryRates");
            assertThat(rates).isEmpty();
        }
    }
}
