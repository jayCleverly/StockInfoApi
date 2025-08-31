package com.github.jaycleverly.stock_info.exception;

/**
 * Custom exception to throw when handling errors in the dynamo client
 */
public class DynamoClientException extends RuntimeException {
    public DynamoClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
