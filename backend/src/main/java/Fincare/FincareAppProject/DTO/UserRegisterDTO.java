package Fincare.FincareAppProject.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class UserRegisterDTO {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Birthdate is required")
    private String birthDate;

    @NotNull(message = "Monthly total income is required")
    @Positive(message = "Monthly total income must be positive")
    private Double month_TotalIncome;

    @NotNull(message = "Monthly fixed expense is required")
    @Positive(message = "Monthly fixed expense must be positive")
    private Double month_FixedExpense;

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    public Double getMonth_TotalIncome() {
        return month_TotalIncome;
    }

    public void setMonth_TotalIncome(Double month_TotalIncome) {
        this.month_TotalIncome = month_TotalIncome;
    }

    public Double getMonth_FixedExpense() {
        return month_FixedExpense;
    }

    public void setMonth_FixedExpense(Double month_FixedExpense) {
        this.month_FixedExpense = month_FixedExpense;
    }
}
