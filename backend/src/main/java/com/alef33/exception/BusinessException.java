package com.alef33.exception;

// Thrown when a business rule is broken (e.g. converting a rejected quote)
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}