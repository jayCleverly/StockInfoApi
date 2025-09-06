package com.github.jaycleverly.stock_info.exception;

/**
 * Custom exception to throw when handling errors caused by converting objects to json
 */
public class SerializerException extends RuntimeException {
    public SerializerException(String message, Throwable cause) {
        super(message, cause);
    }
}
