package com.financial.engine.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "account_balances")
public class AccountBalance {

    @Id
    @Column(name = "account_id", length = 64, nullable = false)
    private String accountId;

    @Column(name = "user_id", length = 64, nullable = false)
    private String userId;

    @Column(name = "total_equity", precision = 18, scale = 2, nullable = false)
    private BigDecimal totalEquity = BigDecimal.ZERO;

    @Column(name = "free_margin", precision = 18, scale = 2, nullable = false)
    private BigDecimal freeMargin = BigDecimal.ZERO;

    @Column(name = "locked_margin", precision = 18, scale = 2, nullable = false)
    private BigDecimal lockedMargin = BigDecimal.ZERO;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public AccountBalance() {
    }

    public AccountBalance(String accountId, String userId, BigDecimal totalEquity, BigDecimal freeMargin, BigDecimal lockedMargin) {
        this.accountId = accountId;
        this.userId = userId;
        this.totalEquity = totalEquity;
        this.freeMargin = freeMargin;
        this.lockedMargin = lockedMargin;
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

    public BigDecimal getTotalEquity() {
        return totalEquity;
    }

    public void setTotalEquity(BigDecimal totalEquity) {
        this.totalEquity = totalEquity;
    }

    public BigDecimal getFreeMargin() {
        return freeMargin;
    }

    public void setFreeMargin(BigDecimal freeMargin) {
        this.freeMargin = freeMargin;
    }

    public BigDecimal getLockedMargin() {
        return lockedMargin;
    }

    public void setLockedMargin(BigDecimal lockedMargin) {
        this.lockedMargin = lockedMargin;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
