package com.github.jaycleverly.stock_info.exception;

import org.springframework.http.HttpStatus;

public class InternalServerErrorException extends RuntimeException {
    private final HttpStatus status;

    public InternalServerErrorException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
