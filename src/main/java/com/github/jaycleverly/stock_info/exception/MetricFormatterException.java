package com.github.jaycleverly.stock_info.exception;

/**
 * Custom exception to throw when handling errors caused by converting metrics to json
 */
public class MetricFormatterException extends RuntimeException {
    public MetricFormatterException(String message, Throwable cause) {
        super(message, cause);
    }
}
