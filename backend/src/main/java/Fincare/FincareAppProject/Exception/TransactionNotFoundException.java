package Fincare.FincareAppProject.Exception;

import org.springframework.http.HttpStatus;

public class TransactionNotFoundException extends FinanceException {
    public TransactionNotFoundException(Long id) {
        super("거래 내역을 찾을 수 없습니다: " + id, HttpStatus.NOT_FOUND);
    }
}
