package com.financial.engine.enums;

public enum OrderStatus {
    PENDING_RISK_CHECK,
    MARGIN_LOCKED,
    OPEN,
    FILLED,
    REJECTED_INSUFFICIENT_MARGIN,
    REJECTED_COOLDOWN,
    REJECTED_LIMIT_EXCEEDED,
    CANCELLED,
    CLOSED
}
