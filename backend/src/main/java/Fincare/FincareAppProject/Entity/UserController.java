package Fincare.FincareAppProject.Entity;

import Fincare.FincareAppProject.DTO.UpdateBudgetDTO;
import Fincare.FincareAppProject.DTO.UserResponseDTO;
import Fincare.FincareAppProject.Service.TransactionService;
import Fincare.FincareAppProject.Repository.UserRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;

@RestController
public class UserController {

    private final UserRepository userRepository;
    private final TransactionService transactionService;

    public UserController(UserRepository userRepository, TransactionService transactionService) {
        this.userRepository = userRepository;
        this.transactionService = transactionService;
    }

    @GetMapping("/user-info")
    public UserResponseDTO getLoggedInUserInfo(@AuthenticationPrincipal String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

        double monthBudget = user.getMonth_TotalIncome() - user.getMonth_FixedExpense();
        int daysInMonth = today.lengthOfMonth();
        double initialDailyBudget = monthBudget / daysInMonth;

        // ✅ 현재 하루 사용 가능 금액 확인
        System.out.println("🚀 Before update - User Info DailyBudget: " + user.getCurrentDailyBudget());

        if (user.getLastUpdatedDate() == null || !user.getLastUpdatedDate().isEqual(today)) {
            double adjustedDailyBudget = transactionService.getDailyAdjustedBudget(username, today);
            if (adjustedDailyBudget > 0) {
                user.setSafeBox(user.getSafeBox() + adjustedDailyBudget);
            }
            user.setCurrentDailyBudget(initialDailyBudget);
            user.setLastUpdatedDate(today);
            userRepository.save(user);
        }

        double adjustedDailyBudget = transactionService.getDailyAdjustedBudget(username, today);

        // ✅ 최종 값 확인
        System.out.println("🚀 Final Daily Budget in UserInfo: " + adjustedDailyBudget);

        return new UserResponseDTO(
                user.getName(),
                user.getBirthDate(),
                monthBudget,
                adjustedDailyBudget,
                user.getSafeBox()
        );
    }


    @GetMapping("/test-reset-daily-budget")
    public String testResetDailyBudget(@AuthenticationPrincipal String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

        // 🔹 초기 하루 사용 가능 금액 계산
        double monthBudget = user.getMonth_TotalIncome() - user.getMonth_FixedExpense();
        int daysInMonth = today.lengthOfMonth();
        double initialDailyBudget = monthBudget / daysInMonth;

        // 🔹 현재 최종 하루 사용 가능 금액을 가져옴 (수입/지출 반영)
        double adjustedDailyBudget = transactionService.getDailyAdjustedBudget(username, today);

        // 🔹 만약 최종 하루 사용 가능 금액이 남아 있다면, 세이프박스에 적립
        if (adjustedDailyBudget > 0) {
            user.setSafeBox(user.getSafeBox() + adjustedDailyBudget);
        }

        // 🔹 강제로 하루 사용 가능 금액 초기화 (자정이 지난 것으로 설정)
        user.setCurrentDailyBudget(initialDailyBudget);
        user.setLastUpdatedDate(today); // 강제로 하루 전 날짜로 변경 (테스트용)
        userRepository.save(user);

        return "✅ 테스트 완료: 최종 남은 금액이 세이프박스에 적립되었고, 하루 사용 가능 금액이 초기화되었습니다!";
    }


    @PatchMapping("/reset-safe-box")
    public String resetSafeBox(@AuthenticationPrincipal String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // 🔹 세이프박스 초기화 (0원으로 설정)
        user.setSafeBox(0.0);
        userRepository.save(user);

        return "✅ 세이프박스가 정상적으로 초기화되었습니다!";
    }

    @GetMapping("/test-reset-to-yesterday")
    public String testResetToYesterday(@AuthenticationPrincipal String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        LocalDate yesterday = LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(1);

        // 🔹 강제로 2월14일로 되돌림
        user.setLastUpdatedDate(yesterday);
        userRepository.save(user);

        return "✅ 테스트 완료: 날짜를 2월14일로 되돌렸습니다!";
    }

    // 🔹 한 달 예산 수정 (월 총 수입 및 고정 지출 수정 가능)
    // 🔹 한 달 예산 수정 API
    @PatchMapping("/update-budget")
    public String updateMonthlyBudget(
            @AuthenticationPrincipal String username,
            @RequestBody UpdateBudgetDTO updateBudgetDTO
    ) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // ✅ 한 달 총 수입과 고정 지출 수정
        user.setMonth_TotalIncome(updateBudgetDTO.getMonthTotalIncome());
        user.setMonth_FixedExpense(updateBudgetDTO.getMonthFixedExpense());

        // ✅ 한 달 예산 변경 후, 새로운 하루 예산 재계산
        double monthBudget = user.getMonth_TotalIncome() - user.getMonth_FixedExpense();
        int daysInMonth = LocalDate.now().lengthOfMonth();
        double newDailyBudget = monthBudget / daysInMonth;

        // ✅ 기존 `currentDailyBudget`을 새로운 `newDailyBudget`으로 업데이트
        user.setCurrentDailyBudget(newDailyBudget);

        userRepository.save(user); // ✅ 변경 사항 저장

        return "월 예산이 성공적으로 변경되었으며, 새로운 하루 사용 가능 금액이 적용되었습니다.";
    }

}