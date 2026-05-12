package Fincare.FincareAppProject.Exception;

import org.springframework.http.HttpStatus;

public class InsufficientSafeBoxException extends FinanceException {
    public InsufficientSafeBoxException() {
        super("SafeBox 잔액이 부족합니다.", HttpStatus.BAD_REQUEST);
    }
}
