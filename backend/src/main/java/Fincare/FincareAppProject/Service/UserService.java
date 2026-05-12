package Fincare.FincareAppProject.Service;

import Fincare.FincareAppProject.DTO.UserRegisterDTO;
import Fincare.FincareAppProject.Config.JwtUtil;
import Fincare.FincareAppProject.Entity.User;
import Fincare.FincareAppProject.Repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    public String register(UserRegisterDTO userRegisterDTO) {
        String encodedPassword = passwordEncoder.encode(userRegisterDTO.getPassword());

        double monthBudget = userRegisterDTO.getMonth_TotalIncome() - userRegisterDTO.getMonth_FixedExpense();
        int daysInMonth = LocalDate.now().lengthOfMonth();  // ✅ 현재 달의 일수 가져오기
        double initialDailyBudget = monthBudget / daysInMonth;  // ✅ 초기 하루 사용 가능 금액 계산

        User user = new User();
        user.setUsername(userRegisterDTO.getUsername());
        user.setPassword(encodedPassword);
        user.setName(userRegisterDTO.getName());
        user.setBirthDate(userRegisterDTO.getBirthDate());
        user.setMonth_TotalIncome(userRegisterDTO.getMonth_TotalIncome());
        user.setMonth_FixedExpense(userRegisterDTO.getMonth_FixedExpense());
        user.setCurrentDailyBudget(initialDailyBudget);  // ✅ 하루 사용 가능 금액 설정
        user.setSafeBox(0.0);

        userRepository.save(user);

        // 🚀 디버깅 로그 추가
        System.out.println("✅ 회원가입 완료 - 초기 하루 예산: " + initialDailyBudget);
        System.out.println("✅ 저장된 currentDailyBudget: " + user.getCurrentDailyBudget());

        return "User registered successfully";
    }



    public String login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        return jwtUtil.generateToken(username);
    }
}
