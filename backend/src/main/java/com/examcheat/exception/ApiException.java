package com.examcheat.exception;

/** Business error that carries the HTTP status to return (404, 409, 401, 403, 429 ...). */
public class ApiException extends RuntimeException {
    private final int status;

    public ApiException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
