package Fincare.FincareAppProject.Controller;

import Fincare.FincareAppProject.DTO.MonthlyTrendDTO;
import Fincare.FincareAppProject.Service.AnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    @Operation(summary = "월별 수입·지출 트렌드",
            description = "최근 N개월(기본 6개월)의 월별 수입·지출 추이를 반환합니다.")
    @GetMapping("/monthly-trend")
    public ResponseEntity<List<MonthlyTrendDTO>> getMonthlyTrend(
            @AuthenticationPrincipal String username,
            @RequestParam(defaultValue = "6") int months) {
        return ResponseEntity.ok(analysisService.getMonthlyTrend(username, months));
    }

    @Operation(summary = "전월 대비 증감률",
            description = "이번 달과 전월의 수입·지출을 비교하여 증감액과 증감률(%)을 반환합니다.")
    @GetMapping("/compare")
    public ResponseEntity<Map<String, Object>> getMonthlyComparison(
            @AuthenticationPrincipal String username) {
        return ResponseEntity.ok(analysisService.getMonthlyComparison(username));
    }

    @Operation(summary = "카테고리별 지출 분석",
            description = "특정 월의 카테고리별 지출 금액과 비율(%)을 반환합니다.")
    @GetMapping("/category")
    public ResponseEntity<Map<String, Object>> getCategoryBreakdown(
            @AuthenticationPrincipal String username,
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(analysisService.getCategoryBreakdown(username, year, month));
    }
}
