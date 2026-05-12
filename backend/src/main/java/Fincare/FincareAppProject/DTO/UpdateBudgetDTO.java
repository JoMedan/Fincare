package Fincare.FincareAppProject.DTO;

public class UpdateBudgetDTO {
    private double monthTotalIncome;
    private double monthFixedExpense;

    public double getMonthTotalIncome() {
        return monthTotalIncome;
    }

    public void setMonthTotalIncome(double monthTotalIncome) {
        this.monthTotalIncome = monthTotalIncome;
    }

    public double getMonthFixedExpense() {
        return monthFixedExpense;
    }

    public void setMonthFixedExpense(double monthFixedExpense) {
        this.monthFixedExpense = monthFixedExpense;
    }
}
