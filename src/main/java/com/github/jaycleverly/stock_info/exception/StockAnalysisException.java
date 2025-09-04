package com.github.jaycleverly.stock_info.exception;

/**
 * Custom exception to throw when handling errors in the stock analysis service
 */
public class StockAnalysisException extends RuntimeException {
    public StockAnalysisException(String message, Throwable cause) {
        super(message, cause);
    }
}
