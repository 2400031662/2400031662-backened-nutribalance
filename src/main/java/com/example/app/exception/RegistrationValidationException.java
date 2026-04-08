package com.example.app.exception;

public class RegistrationValidationException extends RuntimeException {
    public RegistrationValidationException(String message) {
        super(message);
    }
}
