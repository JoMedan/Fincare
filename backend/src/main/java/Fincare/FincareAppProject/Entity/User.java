package Fincare.FincareAppProject.Entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;


    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private  String birthDate;


    @Column(nullable = false)
    private double month_TotalIncome; // 한달 총 수입


    @Column(nullable = false)
    private double month_FixedExpense; // 한달 고정 지출

    @Column(nullable = false)
    private double currentDailyBudget; // 변동된 하루 사용 가능 금액

    @Column(nullable = true)
    private LocalDate lastUpdatedDate = LocalDate.now();

    @Column(nullable = false)
    private double safeBox;

    public double getSafeBox(){
        return safeBox;
    }

    public void setSafeBox(double safeBox){
        this.safeBox=safeBox;
    }


    public double getCurrentDailyBudget() {
        return currentDailyBudget;
    }

    public void setCurrentDailyBudget(double currentDailyBudget) {
        this.currentDailyBudget = currentDailyBudget;
    }


    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getName(){ return  name;}

    public String getBirthDate() { return  birthDate;}

    public double getMonth_TotalIncome(){return  month_TotalIncome;}

    public double getMonth_FixedExpense(){return month_FixedExpense;}

    public void setName(String name){this.name= name;}

    public void setBirthDate(String birthDate) { this.birthDate= birthDate;}

    public void setMonth_TotalIncome(Double monthTotalIncome){ this.month_TotalIncome = monthTotalIncome;}
    public void setMonth_FixedExpense(Double monthFixedExpense){this.month_FixedExpense = monthFixedExpense;}

    public LocalDate getLastUpdatedDate() {
        return lastUpdatedDate;
    }

    public void setLastUpdatedDate(LocalDate lastUpdatedDate) {
        this.lastUpdatedDate = lastUpdatedDate;
    }




}
