package com.alef33.exception;

// Thrown when the email or password is wrong, or the account is disabled
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
