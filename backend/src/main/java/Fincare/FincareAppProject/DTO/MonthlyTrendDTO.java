package Fincare.FincareAppProject.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MonthlyTrendDTO {
    private int year;
    private int month;
    private double incomeTotal;
    private double expenseTotal;
    private double netAmount;
}
