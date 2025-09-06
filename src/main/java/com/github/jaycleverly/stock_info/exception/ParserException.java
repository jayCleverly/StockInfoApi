package com.github.jaycleverly.stock_info.exception;

/**
 * Custom exception to throw when handling errors connected to parsing json
 */
public class ParserException extends RuntimeException {
    public ParserException(String message, Throwable cause) {
        super(message, cause);
    }
}
