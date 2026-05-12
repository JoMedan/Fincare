package Fincare.FincareAppProject.DTO;

import Fincare.FincareAppProject.Enums.TransactionType;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDTO {

    private Long id;

    @NotNull(message = "거래 유형은 필수입니다.")
    private TransactionType type;

    @NotNull(message = "카테고리는 필수입니다.")
    private String category;

    @Positive(message = "금액은 0보다 커야 합니다.")
    private double amount;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @NotNull(message = "날짜는 필수입니다.")
    private LocalDate date;
}
