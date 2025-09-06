package com.github.jaycleverly.stock_info.exception;

/**
 * Custom exception to throw when handling errors in the fake API service
 */
public class FakeApiException extends RuntimeException {
    public FakeApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
