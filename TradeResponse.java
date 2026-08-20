package com.financial.engine.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class TradeResponse {

    private String transactionId;
    private String status;
    private BigDecimal requiredMargin;
    private BigDecimal remainingFreeMargin;
    private BigDecimal currentLockedMargin;
    private OffsetDateTime timestamp;

    public TradeResponse() {
    }

    public TradeResponse(String transactionId, String status, BigDecimal requiredMargin, BigDecimal remainingFreeMargin, BigDecimal currentLockedMargin) {
        this.transactionId = transactionId;
        this.status = status;
        this.requiredMargin = requiredMargin;
        this.remainingFreeMargin = remainingFreeMargin;
        this.currentLockedMargin = currentLockedMargin;
        this.timestamp = OffsetDateTime.now();
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getRequiredMargin() {
        return requiredMargin;
    }

    public void setRequiredMargin(BigDecimal requiredMargin) {
        this.requiredMargin = requiredMargin;
    }

    public BigDecimal getRemainingFreeMargin() {
        return remainingFreeMargin;
    }

    public void setRemainingFreeMargin(BigDecimal remainingFreeMargin) {
        this.remainingFreeMargin = remainingFreeMargin;
    }

    public BigDecimal getCurrentLockedMargin() {
        return currentLockedMargin;
    }

    public void setCurrentLockedMargin(BigDecimal currentLockedMargin) {
        this.currentLockedMargin = currentLockedMargin;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
