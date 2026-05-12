package Fincare.FincareAppProject.Controller;

import Fincare.FincareAppProject.DTO.TransactionDTO;
import Fincare.FincareAppProject.Entity.User;
import Fincare.FincareAppProject.Service.TransactionService;
import Fincare.FincareAppProject.Repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final UserRepository userRepository;

    public TransactionController(TransactionService transactionService, UserRepository userRepository) {
        this.transactionService = transactionService;
        this.userRepository = userRepository;
    }

    // 🔹 새로운 거래 내역 생성
//    @PostMapping
//    public String createTransaction(
//            @AuthenticationPrincipal String username,
//            @RequestBody TransactionDTO transactionDTO,
//            @RequestParam boolean useSafeBox // ✅ SafeBox 사용 여부 플래그 추가
//    ) {
//        transactionService.createTransaction(username, transactionDTO, useSafeBox);
//        return "Transaction created successfully";
//    }

    // 🔹 특정 날짜 조회, 특정 날짜 사이 조회
    @GetMapping("/all")
    public List<TransactionDTO> getTransactions(
            @AuthenticationPrincipal String username,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        return transactionService.getTransactions(username, date, startDate, endDate);
    }

    // 🔹 하루 총 수입/지출 조회
    @GetMapping("/daily")
    public Map<String, Object> getDailyTotals(
            @AuthenticationPrincipal String username,
            @RequestParam(required = false) String date
    ) {
        LocalDate localDate = date != null ? LocalDate.parse(date) : LocalDate.now();
        Map<String, Object> dailyTotals = new HashMap<>(transactionService.getDailyTotals(username, localDate));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // ✅ 초기 하루 예산 (변경 전)
        double monthBudget = user.getMonth_TotalIncome() - user.getMonth_FixedExpense();
        int daysInMonth = localDate.lengthOfMonth();
        double dailyBudgetNoChange = monthBudget / daysInMonth;

        // ✅ 실제 반영된 하루 예산 (세이프박스 적용된 값)
        double currentDailyBudget = user.getCurrentDailyBudget();

        dailyTotals.put("daily_budget_no_change", dailyBudgetNoChange);
        dailyTotals.put("current_daily_budget", currentDailyBudget); // ✅ 최신 하루 예산 추가
        return dailyTotals;
    }


    // 🔹 최종 하루 사용 가능 금액 계산
    @GetMapping("/daily-adjusted-budget")
    public double getDailyAdjustedBudget(
            @AuthenticationPrincipal String username,
            @RequestParam(required = false) String date
    ) {
        LocalDate localDate = date != null ? LocalDate.parse(date) : LocalDate.now();
        return transactionService.getDailyAdjustedBudget(username, localDate);
    }

    // 🔹 특정 월의 누적 지출/수입 조회
    @GetMapping("/monthly-summary")
    public Map<String, Object> getMonthlySummary(
            @AuthenticationPrincipal String username,
            @RequestParam(required = false) String date
    ) {
        LocalDate currentDate = (date != null) ? LocalDate.parse(date) : LocalDate.now();
        return transactionService.getMonthlySummary(username, currentDate);
    }

    // 🔹 특정 거래 내역(수입/지출) 수정
    @PatchMapping("/update/{transactionId}")
    public ResponseEntity<Map<String, Object>> updateTransaction(
            @AuthenticationPrincipal String username,
            @PathVariable Long transactionId,
            @RequestBody TransactionDTO transactionDTO
    ) {
        return transactionService.updateTransaction(username, transactionId, transactionDTO);
    }


    // 🔹 특정 거래 내역(수입/지출) 삭제
    @DeleteMapping("/delete/{transactionId}")
    public String deleteTransaction(
            @AuthenticationPrincipal String username,
            @PathVariable Long transactionId
    ) {
        transactionService.deleteTransaction(username, transactionId);
        return "Transaction deleted successfully";
    }

    @PostMapping("/no-safe-box")
    public String createTransactionWithoutSafeBox(
            @AuthenticationPrincipal String username,
            @RequestBody TransactionDTO transactionDTO
    ) {
        transactionService.createTransactionWithoutSafeBox(username, transactionDTO);
        return "Transaction created successfully without SafeBox";
    }


    @PostMapping("/use-safe-box")
    public String createTransactionUsingSafeBox(
            @AuthenticationPrincipal String username,
            @RequestBody TransactionDTO transactionDTO
    ) {
        transactionService.createTransaction(username, transactionDTO);
        return "Transaction created successfully using SafeBox";
    }

    // 🔹 저번달의 누적 지출/수입 조회 API
    @GetMapping("/last-month-summary")
    public Map<String, Object> getLastMonthSummary(@AuthenticationPrincipal String username) {
        return transactionService.getLastMonthSummary(username);
    }

    // 🔹 특정 월의 지출/수입 내역 조회 API
    @GetMapping("/monthly-details")
    public Map<String, Object> getMonthlyDetails(
            @AuthenticationPrincipal String username,
            @RequestParam Integer year,
            @RequestParam Integer month
    ) {
        return transactionService.getMonthlyDetails(username, year, month);
    }

    @GetMapping("budget-info")
    public Map<String,Object> getUserBudgetInfo(@AuthenticationPrincipal String username){
        User user= userRepository.findByUsername(username).orElseThrow(()->new IllegalArgumentException("사용자를 찾을 수 없음"));
        return Map.of(
                "한달 예산", user.getMonth_TotalIncome(),
                "고정 지출",user.getMonth_FixedExpense()
        );
    }




}
