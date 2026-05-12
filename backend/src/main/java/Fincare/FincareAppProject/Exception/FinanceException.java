package Fincare.FincareAppProject.Exception;

import org.springframework.http.HttpStatus;

public class FinanceException extends RuntimeException {

    private final HttpStatus status;

    public FinanceException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
