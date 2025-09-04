package com.github.jaycleverly.stock_info.exception;

/**
 * Custom exception to throw when handling errors connected to problems with the metric builder service
 */
public class MetricBuilderException extends RuntimeException {
    public MetricBuilderException(String message, Throwable cause) {
        super(message, cause);
    }
}
