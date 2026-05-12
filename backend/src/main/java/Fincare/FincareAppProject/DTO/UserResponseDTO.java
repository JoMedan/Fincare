package Fincare.FincareAppProject.DTO;

public class UserResponseDTO {
    private String name;
    private String birthDate;
    private double monthBudget;
    private double dailyBudget;
    private double safeBox; // ✅ safeBox 필드 추가

    // Constructor
    public UserResponseDTO(String name, String birthDate, double monthBudget, double dailyBudget, double safeBox) {
        this.name = name;
        this.birthDate = birthDate;
        this.monthBudget = monthBudget;
        this.dailyBudget = dailyBudget;
        this.safeBox = safeBox; // ✅ safeBox 초기화 추가
    }

    // Getters and Setters
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

    public double getMonthBudget() {
        return monthBudget;
    }

    public void setMonthBudget(double monthBudget) {
        this.monthBudget = monthBudget;
    }

    public double getDailyBudget() {
        return dailyBudget;
    }

    public void setDailyBudget(double dailyBudget) {
        this.dailyBudget = dailyBudget;
    }

    public double getSafeBox() { // ✅ safeBox Getter 추가
        return safeBox;
    }

    public void setSafeBox(double safeBox) { // ✅ safeBox Setter 추가
        this.safeBox = safeBox;
    }
}
