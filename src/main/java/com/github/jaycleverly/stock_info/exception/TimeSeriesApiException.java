package com.github.jaycleverly.stock_info.exception;

/**
 * Custom exception to throw when handling errors with the time series api client
 */
public class TimeSeriesApiException extends RuntimeException {
    private final int statusCode;
    
    public TimeSeriesApiException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
