package com.github.jaycleverly.stock_info.handler;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.github.jaycleverly.stock_info.exception.ClientErrorException;
import com.github.jaycleverly.stock_info.exception.InternalServerErrorException;

/**
 * Handles exceptions for spring controller
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Returns a 4xx error with associated details
     * 
     * @param exception the exception type that triggers this handle
     * @return a response containing the 4xx error code and error details
     */
    @ExceptionHandler(ClientErrorException.class)
    public ResponseEntity<Map<String, Object>> handleBadUserInput(ClientErrorException exception) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", exception.getMessage());
        body.put("timestamp", LocalDateTime.now());
        body.put("status", exception.getStatus());

        return ResponseEntity.status(exception.getStatus()).body(body);
    }

    /**
     * Returns a 5xx error with associated details
     * 
     * @param exception the exception type that triggers this handle
     * @return a response containing the 500 error code and error details
     */
    @ExceptionHandler(InternalServerErrorException.class)
    public ResponseEntity<Map<String, Object>> handleInternalServerError(InternalServerErrorException exception) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", exception.getMessage());
        body.put("timestamp", LocalDateTime.now());
        body.put("status", exception.getStatus());

        return ResponseEntity.status(exception.getStatus()).body(body);
    }
}
