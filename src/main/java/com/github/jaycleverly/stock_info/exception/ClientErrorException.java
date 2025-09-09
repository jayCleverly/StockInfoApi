package com.github.jaycleverly.stock_info.exception;

import org.springframework.http.HttpStatus;

public class ClientErrorException extends RuntimeException {
    private final HttpStatus status;

    public ClientErrorException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
