package Fincare.FincareAppProject.Service;

import Fincare.FincareAppProject.DTO.TransactionDTO;
import Fincare.FincareAppProject.Entity.Transaction;
import Fincare.FincareAppProject.Repository.TransactionRepository;
import Fincare.FincareAppProject.Entity.User;
import Fincare.FincareAppProject.Repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TransactionService {


    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public TransactionService(TransactionRepository transactionRepository, UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }


    // Create a new transaction
    public void createTransaction(String username, TransactionDTO transactionDTO) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        double currentDailyBudget = user.getCurrentDailyBudget();
        double transactionAmount = transactionDTO.getAmount();

        System.out.println("🚀 [createTransaction] 기존 하루 예산: " + currentDailyBudget);
        System.out.println("🛠 [createTransaction] 트랜잭션 타입: " + transactionDTO.getType() + ", 금액: " + transactionAmount);

        if ("지출".equals(transactionDTO.getType())) {
            if (transactionAmount > currentDailyBudget) {
                double deficit = transactionAmount - currentDailyBudget; // 초과 금액
                if (user.getSafeBox() >= deficit) {
                    user.setSafeBox(user.getSafeBox() - deficit);
                    user.setCurrentDailyBudget(0);
                    System.out.println("✅ [A 선택지] SafeBox 사용 - 새 SafeBox 잔액: " + user.getSafeBox());
                } else {
                    throw new IllegalArgumentException("SafeBox 잔액이 부족합니다.");
                }
            } else {
                user.setCurrentDailyBudget(currentDailyBudget - transactionAmount);
            }
        } else if ("수입".equals(transactionDTO.getType())) {
            user.setCurrentDailyBudget(currentDailyBudget + transactionAmount);
        }

        System.out.println("🔥 [createTransaction] 업데이트된 하루 예산: " + user.getCurrentDailyBudget());

        userRepository.save(user);
        saveTransaction(user, transactionDTO);
    }

    // 🔹 특정 월의 지출/수입 내역 조회
    public Map<String, Object> getMonthlyDetails(String username, int year, int month) {
        LocalDate firstDayOfMonth = LocalDate.of(year, month, 1);
        LocalDate lastDayOfMonth = firstDayOfMonth.withDayOfMonth(firstDayOfMonth.lengthOfMonth());

        // 특정 월의 모든 거래 조회
        List<Transaction> transactions = transactionRepository.findByUserAndDateRange(username, firstDayOfMonth, lastDayOfMonth);

        // ✅ 디버깅 로그
        System.out.println("🚀 [getMonthlyDetails] " + year + "년 " + month + "월 데이터 반환");
        System.out.println("🔹 가져온 거래 개수: " + transactions.size());

        // 거래 내역을 DTO로 변환
        List<TransactionDTO> transactionDTOs = transactions.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        // 결과 반환 (거래 내역 전체)
        return Map.of(
                "year", year,
                "month", month,
                "transactions", transactionDTOs // 해당 월의 모든 거래 내역 반환
        );
    }


    public void createTransactionWithoutSafeBox(String username, TransactionDTO transactionDTO) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        double currentDailyBudget = user.getCurrentDailyBudget();
        double transactionAmount = transactionDTO.getAmount();

        if ("지출".equals(transactionDTO.getType())) {
            if (transactionAmount > currentDailyBudget) {
                double deficit = transactionAmount - currentDailyBudget; // 초과된 금액
                user.setCurrentDailyBudget(currentDailyBudget - transactionAmount); // ✅ 초과된 금액을 그대로 음수로 반영
                System.out.println("✅ [B 선택지] SafeBox 미사용 - 새 하루 예산: " + user.getCurrentDailyBudget());
            } else {
                user.setCurrentDailyBudget(currentDailyBudget - transactionAmount);
            }
        } else if ("수입".equals(transactionDTO.getType())) {
            user.setCurrentDailyBudget(currentDailyBudget + transactionAmount);
        }

        userRepository.save(user);
        saveTransaction(user, transactionDTO);
    }

    private void saveTransaction(User user, TransactionDTO transactionDTO) {
        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setType(transactionDTO.getType());
        transaction.setCategory(transactionDTO.getCategory());
        transaction.setAmount(transactionDTO.getAmount());
        transaction.setDate(transactionDTO.getDate());

        transactionRepository.save(transaction);
    }









    // Retrieve transactions based on filters
    public List<TransactionDTO> getTransactions(String username, String date, String startDate, String endDate) {
        List<Transaction> transactions;

        if (date != null) {
            transactions = transactionRepository.findByUserAndDate(username, LocalDate.parse(date));
        } else if (startDate != null && endDate != null) {
            transactions = transactionRepository.findByUserAndDateRange(
                    username, LocalDate.parse(startDate), LocalDate.parse(endDate)
            );
        } else {
            transactions = transactionRepository.findByUser(username);
        }

        return transactions.stream()
                .map(transaction -> new TransactionDTO(
                        transaction.getId(),
                        transaction.getType(),
                        transaction.getCategory(),
                        transaction.getAmount(),
                        transaction.getDate()
                ))
                .collect(Collectors.toList());
    }

    public Map<String, Object> getDailyTotals(String username, LocalDate date) {
        System.out.println("🚀 [TransactionService] getDailyTotals() 호출 - 사용자: " + username + ", 날짜: " + date);

        // 특정 날짜의 모든 거래 내역 조회
        List<Transaction> transactions = transactionRepository.findByUserAndDate(username, date);

        // ✅ 디버깅 로그 - 가져온 거래 내역 개수
        System.out.println("🔹 [DEBUG] 가져온 거래 내역 개수: " + transactions.size());

        // 거래 내역 출력
        for (Transaction t : transactions) {
            System.out.println("  🔹 거래 ID: " + t.getId() + ", 타입: " + t.getType() + ", 금액: " + t.getAmount() + ", 카테고리: " + t.getCategory());
        }

        // 지출 합계 계산
        double dailyExpenseTotal = transactions.stream()
                .filter(transaction -> "지출".equals(transaction.getType())) // "지출"만 필터링
                .mapToDouble(Transaction::getAmount)
                .sum();

        // 수입 합계 계산
        double dailyIncomeTotal = transactions.stream()
                .filter(transaction -> "수입".equals(transaction.getType())) // "수입"만 필터링
                .mapToDouble(Transaction::getAmount)
                .sum();

        // ✅ 현재 사용자의 최신 하루 예산 가져오기
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        double currentDailyBudget = user.getCurrentDailyBudget();
        List<TransactionDTO> transactionDTOs = transactions.stream()
                .map(this::toDTO) // ✅ Transaction -> TransactionDTO 변환
                .collect(Collectors.toList());


        // ✅ 디버깅 로그 - 계산된 값들 출력
        System.out.println("🔹 [DEBUG] 총 지출: " + dailyExpenseTotal);
        System.out.println("🔹 [DEBUG] 총 수입: " + dailyIncomeTotal);
        System.out.println("🔹 [DEBUG] 최신 하루 예산 (현재 반영된 값): " + currentDailyBudget);

        Map<String, Object> response = new HashMap<>();
        response.put("date", date);
        response.put("daily_expense_total", dailyExpenseTotal);
        response.put("daily_income_total", dailyIncomeTotal);
        response.put("current_daily_budget", currentDailyBudget); // ✅ 최신 하루 예산 추가
        response.put("transactions", transactionDTOs);

        // ✅ 최종 API 응답 로그 출력
        System.out.println("🚀 [TransactionService] getDailyTotals() 최종 반환 데이터: " + response);

        return response;
    }


    private TransactionDTO toDTO(Transaction transaction) {
        return new TransactionDTO(
                transaction.getId(),
                transaction.getType(),
                transaction.getCategory(),
                transaction.getAmount(),
                transaction.getDate()
        );
    }

    public double getDailyAdjustedBudget(String username, LocalDate date) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없음"));

        double monthBudget = user.getMonth_TotalIncome() - user.getMonth_FixedExpense();
        int daysInMonth = date.lengthOfMonth();
        double initialDailyBudget = monthBudget / daysInMonth;

        System.out.println("🚀 [getDailyAdjustedBudget] 기존 하루 예산: " + user.getCurrentDailyBudget());
        System.out.println("🛠 [getDailyAdjustedBudget] 초기 하루 예산 계산 값: " + initialDailyBudget);

        if (user.getLastUpdatedDate() == null || !user.getLastUpdatedDate().isEqual(date)) {
            user.setCurrentDailyBudget(initialDailyBudget);
            user.setLastUpdatedDate(date);
            userRepository.save(user);
            System.out.println("✅ [getDailyAdjustedBudget] 하루 초기화 - 새 하루 예산: " + initialDailyBudget);
            return initialDailyBudget;
        }

        System.out.println("🚀 [getDailyAdjustedBudget] 최종 반환 하루 예산: " + user.getCurrentDailyBudget());
        return user.getCurrentDailyBudget();
    }






    public Map<String, Object> getMonthlySummary(String username, LocalDate currentDate) {
        LocalDate firstDayOfMonth = currentDate.withDayOfMonth(1);

        // 현재 날짜까지의 모든 거래 조회
        List<Transaction> transactions = transactionRepository.findByUserAndDateRange(username, firstDayOfMonth, currentDate);

        // 총 지출 및 수입 계산
        double totalExpenses = transactions.stream()
                .filter(t -> "지출".equals(t.getType()))
                .mapToDouble(Transaction::getAmount)
                .sum();

        double totalIncomes = transactions.stream()
                .filter(t -> "수입".equals(t.getType()))
                .mapToDouble(Transaction::getAmount)
                .sum();

        // **카테고리별 지출 계산**
        Map<String, Double> categoryExpenses = transactions.stream()
                .filter(t -> "지출".equals(t.getType())) // 지출만 필터링
                .collect(Collectors.groupingBy(
                        Transaction::getCategory, // 카테고리별 그룹화
                        Collectors.summingDouble(Transaction::getAmount) // 합산 금액
                ));

        // 결과 반환 (총합 + 카테고리별 지출 포함)
        return Map.of(
                "monthly_expense_total", totalExpenses, // 총 지출
                "monthly_income_total", totalIncomes, // 총 수입
                "category_expenses", categoryExpenses // 카테고리별 지출
        );
    }

    public ResponseEntity<Map<String, Object>> updateTransaction(
            @AuthenticationPrincipal String username,
            @PathVariable Long transactionId,
            @RequestBody TransactionDTO transactionDTO) {

        Optional<Transaction> optionalTransaction = transactionRepository.findById(transactionId);
        if (optionalTransaction.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "거래 내역이 존재하지 않습니다."));
        }

        Transaction transaction = optionalTransaction.get();
        if (!transaction.getUser().getUsername().equals(username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "수정 권한이 없습니다."));
        }

        User user = transaction.getUser();
        double originalAmount = transaction.getAmount();
        double newAmount = transactionDTO.getAmount();
        boolean isExpense = "지출".equals(transaction.getType());

        // ✅ 하루 예산 조정 (수입/지출 변경 반영)
        if (isExpense) {
            user.setCurrentDailyBudget(user.getCurrentDailyBudget() + originalAmount - newAmount);
        } else {
            user.setCurrentDailyBudget(user.getCurrentDailyBudget() - originalAmount + newAmount);
        }

        transaction.setType(transactionDTO.getType());
        transaction.setCategory(transactionDTO.getCategory());
        transaction.setAmount(newAmount);
        transactionRepository.save(transaction);
        userRepository.save(user); // ✅ 변경 사항 저장

        return ResponseEntity.ok(Map.of(
                "message", "거래 내역 수정 성공",
                "updatedTransaction", transactionDTO
        ));
    }


    // 🔹 특정 거래 내역 삭제
    public void deleteTransaction(String username, Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("해당 거래 내역을 찾을 수 없습니다."));

        if (!transaction.getUser().getUsername().equals(username)) {
            throw new IllegalArgumentException("사용자가 소유한 거래 내역이 아닙니다.");
        }

        User user = transaction.getUser();
        boolean isExpense = "지출".equals(transaction.getType());

        if (isExpense) {
            double currentDailyBudget = user.getCurrentDailyBudget();
            double safeBoxBalance = user.getSafeBox();

            if (currentDailyBudget == 0) {
                // ✅ SafeBox에서 차감된 금액이 있었던 경우 복구
                double refundAmount = Math.min(transaction.getAmount(), safeBoxBalance);
                user.setSafeBox(user.getSafeBox() + refundAmount);
            } else {
                user.setCurrentDailyBudget(user.getCurrentDailyBudget() + transaction.getAmount());
            }
        }
        transactionRepository.delete(transaction);
        userRepository.save(user);


    }

    public Map<String, Object>  getLastMonthSummary(String username) {
        LocalDate now = LocalDate.now();
        LocalDate firstDayOfLastMonth = now.minusMonths(1).withDayOfMonth(1);
        LocalDate lastDayOfLastMonth = firstDayOfLastMonth.withDayOfMonth(firstDayOfLastMonth.lengthOfMonth());

        // 특정 월의 모든 거래 조회 (저번달)
        List<Transaction> transactions = transactionRepository.findByUserAndDateRange(username, firstDayOfLastMonth, lastDayOfLastMonth);

        // 총 지출 및 수입 계산
        double totalExpenses = transactions.stream()
                .filter(t -> "지출".equals(t.getType()))
                .mapToDouble(Transaction::getAmount)
                .sum();

        double totalIncomes = transactions.stream()
                .filter(t -> "수입".equals(t.getType()))
                .mapToDouble(Transaction::getAmount)
                .sum();

        // **카테고리별 지출 계산**
        Map<String, Double> categoryExpenses = transactions.stream()
                .filter(t -> "지출".equals(t.getType())) // 지출만 필터링
                .collect(Collectors.groupingBy(
                        Transaction::getCategory, // 카테고리별 그룹화
                        Collectors.summingDouble(Transaction::getAmount) // 합산 금액
                ));

        return Map.of(
                "year", firstDayOfLastMonth.getYear(),
                "month", firstDayOfLastMonth.getMonthValue(),
                "last_month_expense_total", totalExpenses, // 저번달 총 지출
                "last_month_income_total", totalIncomes, // 저번달 총 수입
                "category_expenses", categoryExpenses // 카테고리별 지출
        );
    }

}
