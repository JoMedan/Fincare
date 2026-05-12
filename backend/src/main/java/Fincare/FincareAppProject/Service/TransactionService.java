package Fincare.FincareAppProject.Service;

import Fincare.FincareAppProject.DTO.TransactionDTO;
import Fincare.FincareAppProject.Entity.Transaction;
import Fincare.FincareAppProject.Entity.User;
import Fincare.FincareAppProject.Enums.TransactionType;
import Fincare.FincareAppProject.Exception.InsufficientSafeBoxException;
import Fincare.FincareAppProject.Exception.TransactionNotFoundException;
import Fincare.FincareAppProject.Exception.UnauthorizedAccessException;
import Fincare.FincareAppProject.Exception.UserNotFoundException;
import Fincare.FincareAppProject.Repository.TransactionRepository;
import Fincare.FincareAppProject.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Transactional
    public void createTransaction(String username, TransactionDTO dto) {
        User user = findUser(username);

        double currentDailyBudget = user.getCurrentDailyBudget();
        double amount = dto.getAmount();

        if (dto.getType().isExpense()) {
            if (amount > currentDailyBudget) {
                double deficit = amount - currentDailyBudget;
                if (user.getSafeBox() < deficit) {
                    throw new InsufficientSafeBoxException();
                }
                user.setSafeBox(user.getSafeBox() - deficit);
                user.setCurrentDailyBudget(0);
                log.info("SafeBox 사용 - 사용자: {}, 차감액: {}, 잔액: {}", username, deficit, user.getSafeBox());
            } else {
                user.setCurrentDailyBudget(currentDailyBudget - amount);
            }
        } else if (dto.getType().isIncome()) {
            user.setCurrentDailyBudget(currentDailyBudget + amount);
        }

        userRepository.save(user);
        saveTransaction(user, dto);
    }

    @Transactional
    public void createTransactionWithoutSafeBox(String username, TransactionDTO dto) {
        User user = findUser(username);

        double amount = dto.getAmount();
        if (dto.getType().isExpense()) {
            user.setCurrentDailyBudget(user.getCurrentDailyBudget() - amount);
        } else if (dto.getType().isIncome()) {
            user.setCurrentDailyBudget(user.getCurrentDailyBudget() + amount);
        }

        userRepository.save(user);
        saveTransaction(user, dto);
    }

    public List<TransactionDTO> getTransactions(String username, String date, String startDate, String endDate) {
        List<Transaction> transactions;

        if (date != null) {
            transactions = transactionRepository.findByUserAndDate(username, LocalDate.parse(date));
        } else if (startDate != null && endDate != null) {
            transactions = transactionRepository.findByUserAndDateRange(
                    username, LocalDate.parse(startDate), LocalDate.parse(endDate));
        } else {
            transactions = transactionRepository.findByUser(username);
        }

        return transactions.stream().map(this::toDTO).collect(Collectors.toList());
    }

    public Map<String, Object> getDailyTotals(String username, LocalDate date) {
        List<Transaction> transactions = transactionRepository.findByUserAndDate(username, date);

        double dailyExpenseTotal = transactions.stream()
                .filter(t -> t.getType().isExpense())
                .mapToDouble(Transaction::getAmount).sum();

        double dailyIncomeTotal = transactions.stream()
                .filter(t -> t.getType().isIncome())
                .mapToDouble(Transaction::getAmount).sum();

        User user = findUser(username);
        double monthBudget = user.getMonth_TotalIncome() - user.getMonth_FixedExpense();

        Map<String, Object> response = new HashMap<>();
        response.put("date", date);
        response.put("daily_expense_total", dailyExpenseTotal);
        response.put("daily_income_total", dailyIncomeTotal);
        response.put("current_daily_budget", user.getCurrentDailyBudget());
        response.put("daily_budget_no_change", monthBudget / date.lengthOfMonth());
        response.put("transactions", transactions.stream().map(this::toDTO).collect(Collectors.toList()));
        return response;
    }

    @Transactional
    public double getDailyAdjustedBudget(String username, LocalDate date) {
        User user = findUser(username);

        double monthBudget = user.getMonth_TotalIncome() - user.getMonth_FixedExpense();
        double initialDailyBudget = monthBudget / date.lengthOfMonth();

        if (user.getLastUpdatedDate() == null || !user.getLastUpdatedDate().isEqual(date)) {
            user.setCurrentDailyBudget(initialDailyBudget);
            user.setLastUpdatedDate(date);
            userRepository.save(user);
            log.info("하루 예산 초기화 - 사용자: {}, 날짜: {}, 예산: {}", username, date, initialDailyBudget);
            return initialDailyBudget;
        }

        return user.getCurrentDailyBudget();
    }

    public Map<String, Object> getMonthlySummary(String username, LocalDate currentDate) {
        LocalDate firstDay = currentDate.withDayOfMonth(1);
        List<Transaction> transactions = transactionRepository.findByUserAndDateRange(username, firstDay, currentDate);

        return buildSummary(transactions,
                Map.of("monthly_expense_total", sumExpense(transactions),
                        "monthly_income_total", sumIncome(transactions),
                        "category_expenses", categoryExpenseMap(transactions)));
    }

    public Map<String, Object> getLastMonthSummary(String username) {
        LocalDate firstDay = LocalDate.now().minusMonths(1).withDayOfMonth(1);
        LocalDate lastDay = firstDay.withDayOfMonth(firstDay.lengthOfMonth());
        List<Transaction> transactions = transactionRepository.findByUserAndDateRange(username, firstDay, lastDay);

        Map<String, Object> result = new HashMap<>();
        result.put("year", firstDay.getYear());
        result.put("month", firstDay.getMonthValue());
        result.put("last_month_expense_total", sumExpense(transactions));
        result.put("last_month_income_total", sumIncome(transactions));
        result.put("category_expenses", categoryExpenseMap(transactions));
        return result;
    }

    public Map<String, Object> getMonthlyDetails(String username, int year, int month) {
        LocalDate firstDay = LocalDate.of(year, month, 1);
        LocalDate lastDay = firstDay.withDayOfMonth(firstDay.lengthOfMonth());

        List<TransactionDTO> dtos = transactionRepository
                .findByUserAndDateRange(username, firstDay, lastDay)
                .stream().map(this::toDTO).collect(Collectors.toList());

        return Map.of("year", year, "month", month, "transactions", dtos);
    }

    @Transactional
    public ResponseEntity<Map<String, Object>> updateTransaction(String username, Long transactionId, TransactionDTO dto) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));

        if (!transaction.getUser().getUsername().equals(username)) {
            throw new UnauthorizedAccessException();
        }

        User user = transaction.getUser();
        double originalAmount = transaction.getAmount();
        double newAmount = dto.getAmount();

        if (transaction.getType().isExpense()) {
            user.setCurrentDailyBudget(user.getCurrentDailyBudget() + originalAmount - newAmount);
        } else {
            user.setCurrentDailyBudget(user.getCurrentDailyBudget() - originalAmount + newAmount);
        }

        transaction.setType(dto.getType());
        transaction.setCategory(dto.getCategory());
        transaction.setAmount(newAmount);
        transactionRepository.save(transaction);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "거래 내역 수정 성공", "updatedTransaction", dto));
    }

    @Transactional
    public void deleteTransaction(String username, Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));

        if (!transaction.getUser().getUsername().equals(username)) {
            throw new UnauthorizedAccessException();
        }

        User user = transaction.getUser();
        if (transaction.getType().isExpense()) {
            if (user.getCurrentDailyBudget() == 0) {
                user.setSafeBox(user.getSafeBox() + Math.min(transaction.getAmount(), user.getSafeBox()));
            } else {
                user.setCurrentDailyBudget(user.getCurrentDailyBudget() + transaction.getAmount());
            }
        }

        transactionRepository.delete(transaction);
        userRepository.save(user);
    }

    // ── private helpers ──────────────────────────────────

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));
    }

    private void saveTransaction(User user, TransactionDTO dto) {
        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setType(dto.getType());
        transaction.setCategory(dto.getCategory());
        transaction.setAmount(dto.getAmount());
        transaction.setDate(dto.getDate());
        transactionRepository.save(transaction);
    }

    private TransactionDTO toDTO(Transaction t) {
        return new TransactionDTO(t.getId(), t.getType(), t.getCategory(), t.getAmount(), t.getDate());
    }

    private double sumExpense(List<Transaction> list) {
        return list.stream().filter(t -> t.getType().isExpense()).mapToDouble(Transaction::getAmount).sum();
    }

    private double sumIncome(List<Transaction> list) {
        return list.stream().filter(t -> t.getType().isIncome()).mapToDouble(Transaction::getAmount).sum();
    }

    private Map<String, Double> categoryExpenseMap(List<Transaction> list) {
        return list.stream()
                .filter(t -> t.getType().isExpense())
                .collect(Collectors.groupingBy(Transaction::getCategory, Collectors.summingDouble(Transaction::getAmount)));
    }

    private Map<String, Object> buildSummary(List<Transaction> transactions, Map<String, Object> extra) {
        Map<String, Object> result = new HashMap<>(extra);
        return result;
    }
}
