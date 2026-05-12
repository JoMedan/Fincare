package Fincare.FincareAppProject.Exception;

import org.springframework.http.HttpStatus;

public class InvalidPasswordException extends FinanceException {
    public InvalidPasswordException() {
        super("비밀번호가 일치하지 않습니다.", HttpStatus.BAD_REQUEST);
    }
}
