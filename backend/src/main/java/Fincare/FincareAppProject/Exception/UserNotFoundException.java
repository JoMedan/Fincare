package Fincare.FincareAppProject.Exception;

import org.springframework.http.HttpStatus;

public class UserNotFoundException extends FinanceException {
    public UserNotFoundException(String username) {
        super("사용자를 찾을 수 없습니다: " + username, HttpStatus.NOT_FOUND);
    }
}
