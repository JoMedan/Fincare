package Fincare.FincareAppProject.Controller;

import Fincare.FincareAppProject.DTO.TransactionDTO;
import Fincare.FincareAppProject.Service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @Operation(summary = "거래 내역 조회", description = "특정 날짜 또는 기간의 거래 내역을 조회합니다.")
    @GetMapping("/all")
    public List<TransactionDTO> getTransactions(
            @AuthenticationPrincipal String username,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        return transactionService.getTransactions(username, date, startDate, endDate);
    }

    @Operation(summary = "일별 수입/지출 조회", description = "특정 날짜의 수입, 지출 합계와 예산 현황을 조회합니다.")
    @GetMapping("/daily")
    public Map<String, Object> getDailyTotals(
            @AuthenticationPrincipal String username,
            @RequestParam(required = false) String date
    ) {
        LocalDate localDate = date != null ? LocalDate.parse(date) : LocalDate.now();
        return transactionService.getDailyTotals(username, localDate);
    }

    @Operation(summary = "하루 사용 가능 금액 조회", description = "수입/지출을 반영한 최종 하루 사용 가능 금액을 조회합니다.")
    @GetMapping("/daily-adjusted-budget")
    public double getDailyAdjustedBudget(
            @AuthenticationPrincipal String username,
            @RequestParam(required = false) String date
    ) {
        LocalDate localDate = date != null ? LocalDate.parse(date) : LocalDate.now();
        return transactionService.getDailyAdjustedBudget(username, localDate);
    }

    @Operation(summary = "월별 수입/지출 요약", description = "특정 월의 누적 지출/수입과 카테고리별 지출을 조회합니다.")
    @GetMapping("/monthly-summary")
    public Map<String, Object> getMonthlySummary(
            @AuthenticationPrincipal String username,
            @RequestParam(required = false) String date
    ) {
        LocalDate currentDate = date != null ? LocalDate.parse(date) : LocalDate.now();
        return transactionService.getMonthlySummary(username, currentDate);
    }

    @Operation(summary = "지난달 수입/지출 요약", description = "지난달의 누적 지출/수입과 카테고리별 지출을 조회합니다.")
    @GetMapping("/last-month-summary")
    public Map<String, Object> getLastMonthSummary(@AuthenticationPrincipal String username) {
        return transactionService.getLastMonthSummary(username);
    }

    @Operation(summary = "월별 거래 내역 조회", description = "특정 연월의 모든 거래 내역을 조회합니다.")
    @GetMapping("/monthly-details")
    public Map<String, Object> getMonthlyDetails(
            @AuthenticationPrincipal String username,
            @RequestParam Integer year,
            @RequestParam Integer month
    ) {
        return transactionService.getMonthlyDetails(username, year, month);
    }

    @Operation(summary = "거래 내역 수정", description = "특정 거래 내역의 금액, 카테고리 등을 수정합니다.")
    @PatchMapping("/update/{transactionId}")
    public ResponseEntity<Map<String, Object>> updateTransaction(
            @AuthenticationPrincipal String username,
            @PathVariable Long transactionId,
            @RequestBody TransactionDTO transactionDTO
    ) {
        return transactionService.updateTransaction(username, transactionId, transactionDTO);
    }

    @Operation(summary = "거래 내역 삭제", description = "특정 거래 내역을 삭제합니다.")
    @DeleteMapping("/delete/{transactionId}")
    public ResponseEntity<Map<String, String>> deleteTransaction(
            @AuthenticationPrincipal String username,
            @PathVariable Long transactionId
    ) {
        transactionService.deleteTransaction(username, transactionId);
        return ResponseEntity.ok(Map.of("message", "거래 내역이 삭제되었습니다."));
    }

    @Operation(summary = "지출 등록 (세이프박스 미사용)", description = "세이프박스를 사용하지 않고 지출/수입을 등록합니다.")
    @PostMapping("/no-safe-box")
    public ResponseEntity<Map<String, String>> createTransactionWithoutSafeBox(
            @AuthenticationPrincipal String username,
            @RequestBody TransactionDTO transactionDTO
    ) {
        transactionService.createTransactionWithoutSafeBox(username, transactionDTO);
        return ResponseEntity.ok(Map.of("message", "거래 내역이 등록되었습니다."));
    }

    @Operation(summary = "지출 등록 (세이프박스 사용)", description = "세이프박스를 사용하여 지출/수입을 등록합니다.")
    @PostMapping("/use-safe-box")
    public ResponseEntity<Map<String, String>> createTransactionUsingSafeBox(
            @AuthenticationPrincipal String username,
            @RequestBody TransactionDTO transactionDTO
    ) {
        transactionService.createTransaction(username, transactionDTO);
        return ResponseEntity.ok(Map.of("message", "거래 내역이 등록되었습니다."));
    }
}
