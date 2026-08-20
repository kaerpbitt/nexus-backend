package com.financial.engine.entity;

import com.financial.engine.enums.OrderStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @Column(name = "order_id", length = 64, nullable = false)
    private String orderId;

    @Column(name = "account_id", length = 64, nullable = false)
    private String accountId;

    @Column(name = "symbol", length = 32, nullable = false)
    private String symbol;

    @Column(name = "volume", precision = 18, scale = 8, nullable = false)
    private BigDecimal volume;

    @Column(name = "price", precision = 18, scale = 4, nullable = false)
    private BigDecimal price;

    @Column(name = "leverage", precision = 8, scale = 2, nullable = false)
    private BigDecimal leverage;

    @Column(name = "required_margin", precision = 18, scale = 2, nullable = false)
    private BigDecimal requiredMargin;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private OrderStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public Order() {
    }

    public Order(String orderId, String accountId, String symbol, BigDecimal volume, BigDecimal price, BigDecimal leverage, BigDecimal requiredMargin, OrderStatus status) {
        this.orderId = orderId;
        this.accountId = accountId;
        this.symbol = symbol;
        this.volume = volume;
        this.price = price;
        this.leverage = leverage;
        this.requiredMargin = requiredMargin;
        this.status = status;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
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

    public BigDecimal getRequiredMargin() {
        return requiredMargin;
    }

    public void setRequiredMargin(BigDecimal requiredMargin) {
        this.requiredMargin = requiredMargin;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
