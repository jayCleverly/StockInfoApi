package com.github.jaycleverly.stock_info.handler;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.github.jaycleverly.stock_info.exception.StockAnalysisException;

/**
 * Handles exceptions for spring controller
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Returns a 400 error with associated details
     * 
     * @param exception the exception type that triggers this handle
     * @return a response containing the 400 error code and error details
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadUserInput(IllegalArgumentException exception) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Invalid Request");
        body.put("message", exception.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Returns a 500 error with associated details
     * 
     * @param exception the exception type that triggers this handle
     * @return a response containing the 500 error code and error details
     */
    @ExceptionHandler(StockAnalysisException.class)
    public ResponseEntity<Map<String, Object>> handleInternalServerError(StockAnalysisException exception) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", "Internal Server Error");
        body.put("message", exception.getMessage());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
