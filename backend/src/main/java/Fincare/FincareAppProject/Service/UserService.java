package Fincare.FincareAppProject.Service;

import Fincare.FincareAppProject.Config.JwtUtil;
import Fincare.FincareAppProject.DTO.UpdateBudgetDTO;
import Fincare.FincareAppProject.DTO.UserRegisterDTO;
import Fincare.FincareAppProject.DTO.UserResponseDTO;
import Fincare.FincareAppProject.Entity.User;
import Fincare.FincareAppProject.Exception.InvalidPasswordException;
import Fincare.FincareAppProject.Exception.UserNotFoundException;
import Fincare.FincareAppProject.Repository.UserRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final TransactionService transactionService;

    public UserService(UserRepository userRepository, JwtUtil jwtUtil, PasswordEncoder passwordEncoder,
                       @Lazy TransactionService transactionService) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.transactionService = transactionService;
    }

    @Transactional
    public String register(UserRegisterDTO dto) {
        double monthBudget = dto.getMonth_TotalIncome() - dto.getMonth_FixedExpense();
        double initialDailyBudget = monthBudget / LocalDate.now().lengthOfMonth();

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setName(dto.getName());
        user.setBirthDate(dto.getBirthDate());
        user.setMonth_TotalIncome(dto.getMonth_TotalIncome());
        user.setMonth_FixedExpense(dto.getMonth_FixedExpense());
        user.setCurrentDailyBudget(initialDailyBudget);
        user.setSafeBox(0.0);

        userRepository.save(user);
        return "User registered successfully";
    }

    public String login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidPasswordException();
        }

        return jwtUtil.generateToken(username);
    }

    @Transactional
    public UserResponseDTO getUserInfo(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        double monthBudget = user.getMonth_TotalIncome() - user.getMonth_FixedExpense();
        double initialDailyBudget = monthBudget / today.lengthOfMonth();

        if (user.getLastUpdatedDate() == null || !user.getLastUpdatedDate().isEqual(today)) {
            double remaining = transactionService.getDailyAdjustedBudget(username, today);
            if (remaining > 0) {
                user.setSafeBox(user.getSafeBox() + remaining);
            }
            user.setCurrentDailyBudget(initialDailyBudget);
            user.setLastUpdatedDate(today);
            userRepository.save(user);
        }

        double adjustedDailyBudget = transactionService.getDailyAdjustedBudget(username, today);
        return new UserResponseDTO(user.getName(), user.getBirthDate(), monthBudget, adjustedDailyBudget, user.getSafeBox());
    }

    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new InvalidPasswordException();
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public void deleteAccount(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidPasswordException();
        }

        userRepository.delete(user);
    }

    @Transactional
    public void resetSafeBox(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));
        user.setSafeBox(0.0);
        userRepository.save(user);
    }

    @Transactional
    public void updateBudget(String username, UpdateBudgetDTO dto) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        user.setMonth_TotalIncome(dto.getMonthTotalIncome());
        user.setMonth_FixedExpense(dto.getMonthFixedExpense());

        double monthBudget = user.getMonth_TotalIncome() - user.getMonth_FixedExpense();
        user.setCurrentDailyBudget(monthBudget / LocalDate.now().lengthOfMonth());

        userRepository.save(user);
    }

    public Map<String, Object> getBudgetInfo(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));
        return Map.of(
                "monthlyBudget", user.getMonth_TotalIncome(),
                "fixedExpense", user.getMonth_FixedExpense()
        );
    }
}
