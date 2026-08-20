package com.financial.engine.dto;

import java.math.BigDecimal;

public class OrderRequest {

    private String accountId;
    private String userId;
    private String symbol;
    private BigDecimal volume;
    private BigDecimal price;
    private BigDecimal leverage;
    private long maxPositionLimit = 5;

    public OrderRequest() {
    }

    public OrderRequest(String accountId, String userId, String symbol, BigDecimal volume, BigDecimal price, BigDecimal leverage, long maxPositionLimit) {
        this.accountId = accountId;
        this.userId = userId;
        this.symbol = symbol;
        this.volume = volume;
        this.price = price;
        this.leverage = leverage;
        this.maxPositionLimit = maxPositionLimit;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public BigDecimal getVolume() {
        return volume;
    }

    public void setVolume(BigDecimal volume) {
        this.volume = volume;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getLeverage() {
        return leverage;
    }

    public void setLeverage(BigDecimal leverage) {
        this.leverage = leverage;
    }

    public long getMaxPositionLimit() {
        return maxPositionLimit;
    }

    public void setMaxPositionLimit(long maxPositionLimit) {
        this.maxPositionLimit = maxPositionLimit;
    }
}
