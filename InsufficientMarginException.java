package com.financial.engine.exception;

public class InsufficientMarginException extends RuntimeException {
    public InsufficientMarginException(String message) {
        super(message);
    }
}
