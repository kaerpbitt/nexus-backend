package com.financial.engine.exception;

public class RiskPolicyViolationException extends RuntimeException {
    public RiskPolicyViolationException(String message) {
        super(message);
    }
}
