package Fincare.FincareAppProject.Service;

import Fincare.FincareAppProject.DTO.MonthlyTrendDTO;
import Fincare.FincareAppProject.Entity.Transaction;
import Fincare.FincareAppProject.Repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final TransactionRepository transactionRepository;

    /**
     * 최근 N개월의 월별 수입·지출 트렌드를 반환한다.
     * months=6 이면 이번 달 포함 6개월치를 오래된 순으로 반환한다.
     */
    public List<MonthlyTrendDTO> getMonthlyTrend(String username, int months) {
        if (months < 1 || months > 24) {
            months = 6;
        }

        LocalDate today = LocalDate.now();
        LocalDate rangeStart = today.minusMonths(months - 1).withDayOfMonth(1);

        List<Transaction> all = transactionRepository.findByUserAndDateRange(username, rangeStart, today);

        int finalMonths = months;
        return IntStream.range(0, finalMonths)
                .mapToObj(i -> today.minusMonths(finalMonths - 1 - i).withDayOfMonth(1))
                .map(monthStart -> {
                    LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
                    List<Transaction> monthly = all.stream()
                            .filter(t -> !t.getDate().isBefore(monthStart) && !t.getDate().isAfter(monthEnd))
                            .collect(Collectors.toList());
                    double income = sumIncome(monthly);
                    double expense = sumExpense(monthly);
                    return new MonthlyTrendDTO(monthStart.getYear(), monthStart.getMonthValue(),
                            income, expense, income - expense);
                })
                .collect(Collectors.toList());
    }

    /**
     * 이번 달과 전월의 수입·지출을 비교하여 증감액과 증감률을 반환한다.
     */
    public Map<String, Object> getMonthlyComparison(String username) {
        LocalDate today = LocalDate.now();
        LocalDate thisStart = today.withDayOfMonth(1);
        LocalDate lastStart = today.minusMonths(1).withDayOfMonth(1);
        LocalDate lastEnd = lastStart.withDayOfMonth(lastStart.lengthOfMonth());

        List<Transaction> thisTxns = transactionRepository.findByUserAndDateRange(username, thisStart, today);
        List<Transaction> lastTxns = transactionRepository.findByUserAndDateRange(username, lastStart, lastEnd);

        double thisExpense = sumExpense(thisTxns);
        double lastExpense = sumExpense(lastTxns);
        double thisIncome = sumIncome(thisTxns);
        double lastIncome = sumIncome(lastTxns);

        Map<String, Object> result = new HashMap<>();
        result.put("thisMonth", Map.of(
                "year", today.getYear(), "month", today.getMonthValue(),
                "expense", thisExpense, "income", thisIncome));
        result.put("lastMonth", Map.of(
                "year", lastStart.getYear(), "month", lastStart.getMonthValue(),
                "expense", lastExpense, "income", lastIncome));
        result.put("expenseChange", Map.of(
                "amount", thisExpense - lastExpense,
                "rate", calcChangeRate(lastExpense, thisExpense)));
        result.put("incomeChange", Map.of(
                "amount", thisIncome - lastIncome,
                "rate", calcChangeRate(lastIncome, thisIncome)));
        return result;
    }

    /**
     * 특정 월의 카테고리별 지출 금액과 비율을 반환한다.
     */
    public Map<String, Object> getCategoryBreakdown(String username, int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        List<Transaction> txns = transactionRepository.findByUserAndDateRange(username, start, end);

        double totalExpense = sumExpense(txns);

        Map<String, Double> categoryAmounts = txns.stream()
                .filter(t -> t.getType().isExpense())
                .collect(Collectors.groupingBy(
                        Transaction::getCategory,
                        Collectors.summingDouble(Transaction::getAmount)));

        Map<String, Double> categoryRates = categoryAmounts.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> totalExpense > 0 ? round1(e.getValue() / totalExpense * 100) : 0.0));

        Map<String, Object> result = new HashMap<>();
        result.put("year", year);
        result.put("month", month);
        result.put("totalExpense", totalExpense);
        result.put("categoryAmounts", categoryAmounts);
        result.put("categoryRates", categoryRates);
        return result;
    }

    // ── private helpers ──────────────────────────────────

    private double sumExpense(List<Transaction> list) {
        return list.stream().filter(t -> t.getType().isExpense()).mapToDouble(Transaction::getAmount).sum();
    }

    private double sumIncome(List<Transaction> list) {
        return list.stream().filter(t -> t.getType().isIncome()).mapToDouble(Transaction::getAmount).sum();
    }

    private double calcChangeRate(double previous, double current) {
        if (previous == 0) return current > 0 ? 100.0 : 0.0;
        return round1((current - previous) / previous * 100);
    }

    private double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
