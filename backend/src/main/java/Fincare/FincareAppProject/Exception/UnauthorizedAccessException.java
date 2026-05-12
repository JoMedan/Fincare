package Fincare.FincareAppProject.Exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedAccessException extends FinanceException {
    public UnauthorizedAccessException() {
        super("해당 리소스에 대한 접근 권한이 없습니다.", HttpStatus.FORBIDDEN);
    }
}
