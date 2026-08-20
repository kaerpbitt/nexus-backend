package com.financial.engine.service;

import com.financial.engine.core.FinancialEngine;
import com.financial.engine.dto.OrderRequest;
import com.financial.engine.dto.TradeResponse;
import com.financial.engine.entity.*;
import com.financial.engine.enums.LedgerEntryType;
import com.financial.engine.enums.OrderStatus;
import com.financial.engine.exception.*;
import com.financial.engine.repository.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

@Service
public class TradeExecutionService {

    private final AccountBalanceRepository balanceRepository;
    private final OrderRepository orderRepository;
    private final LedgerRepository ledgerRepository;
    private final SystemAuditLogRepository auditLogRepository;
    private final StringRedisTemplate redisTemplate;

    public TradeExecutionService(AccountBalanceRepository balanceRepository,
                                 OrderRepository orderRepository,
                                 LedgerRepository ledgerRepository,
                                 SystemAuditLogRepository auditLogRepository,
                                 StringRedisTemplate redisTemplate) {
        this.balanceRepository = balanceRepository;
        this.orderRepository = orderRepository;
        this.ledgerRepository = ledgerRepository;
        this.auditLogRepository = auditLogRepository;
        this.redisTemplate = redisTemplate;
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public TradeResponse processOrderExecution(OrderRequest request, String ipAddress, String userAgent) {

        // ------------------------------------------------------------------
        // STEP 1: Pre-Trade Risk Governor - Anti-Spam Execution Cooldown
        // ------------------------------------------------------------------
        String cooldownKey = "cooldown:account:" + request.getAccountId();
        Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(cooldownKey, "LOCKED", Duration.ofSeconds(10));
        
        if (Boolean.FALSE.equals(lockAcquired)) {
            recordAuditLog(request.getUserId(), request.getAccountId(), "ORDER_REJECTED_COOLDOWN", ipAddress, userAgent, "Rate limit hit", 429);
            throw new RateLimitExceededException("Execution Cooldown active. Please wait 10 seconds.");
        }

        // ------------------------------------------------------------------
        // STEP 2: DB Atomic Lock (PESSIMISTIC_WRITE)
        // ------------------------------------------------------------------
        AccountBalance balance = balanceRepository.findByAccountIdWithPessimisticLock(request.getAccountId())
                .orElseThrow(() -> new AccountNotFoundException("Account ID not found: " + request.getAccountId()));

        // ------------------------------------------------------------------
        // STEP 3: Pre-Trade Risk Governor - Max Positions Check
        // ------------------------------------------------------------------
        long activeOrders = orderRepository.countActiveOrdersByAccountId(request.getAccountId());
        if (activeOrders >= request.getMaxPositionLimit()) {
            recordAuditLog(request.getUserId(), request.getAccountId(), "ORDER_REJECTED_MAX_POSITIONS", ipAddress, userAgent, "Max limit: " + request.getMaxPositionLimit(), 400);
            throw new RiskPolicyViolationException("Max active position limit reached: " + request.getMaxPositionLimit());
        }

        // ------------------------------------------------------------------
        // STEP 4: Exact Precision Engine Calculation
        // ------------------------------------------------------------------
        BigDecimal requiredMargin = FinancialEngine.calculateRequiredMargin(
                request.getVolume(),
                request.getPrice(),
                request.getLeverage()
        );

        // Pre-Trade Risk Governor - Free Margin Check
        if (!FinancialEngine.hasSufficientMargin(balance.getFreeMargin(), requiredMargin)) {
            recordAuditLog(request.getUserId(), request.getAccountId(), "ORDER_REJECTED_INSUFFICIENT_MARGIN", ipAddress, userAgent, "Required: " + requiredMargin + ", Free: " + balance.getFreeMargin(), 400);
            throw new InsufficientMarginException("Insufficient Free Margin. Required: " + requiredMargin + ", Available: " + balance.getFreeMargin());
        }

        // ------------------------------------------------------------------
        // STEP 5: Ledger State Mutation & Margin Lock (Double-Entry Journal)
        // ------------------------------------------------------------------
        String transactionId = "TX-" + UUID.randomUUID().toString().toUpperCase();

        // Atomic Mutation
        balance.setFreeMargin(balance.getFreeMargin().subtract(requiredMargin));
        balance.setLockedMargin(balance.getLockedMargin().add(requiredMargin));
        balanceRepository.save(balance);

        // Double-Entry Ledger Entry
        LedgerEntry ledgerEntry = new LedgerEntry(
                transactionId,
                request.getAccountId(),
                LedgerEntryType.MARGIN_LOCK,
                requiredMargin, // Debit: Increase Locked Margin
                requiredMargin  // Credit: Decrease Free Margin
        );
        ledgerRepository.save(ledgerEntry);

        // Record Order State
        Order order = new Order(
                transactionId,
                request.getAccountId(),
                request.getSymbol(),
                request.getVolume(),
                request.getPrice(),
                request.getLeverage(),
                requiredMargin,
                OrderStatus.MARGIN_LOCKED
        );
        orderRepository.save(order);

        // System Audit Log
        recordAuditLog(request.getUserId(), request.getAccountId(), "ORDER_MARGIN_LOCKED", ipAddress, userAgent, "TX: " + transactionId + " Locked Margin: " + requiredMargin, 200);

        return new TradeResponse(
                transactionId,
                OrderStatus.MARGIN_LOCKED.name(),
                requiredMargin,
                balance.getFreeMargin(),
                balance.getLockedMargin()
        );
    }

    private void recordAuditLog(String userId, String accountId, String action, String ip, String userAgent, String payload, int statusCode) {
        SystemAuditLog auditLog = new SystemAuditLog(userId, accountId, action, ip, userAgent, payload, statusCode);
        auditLogRepository.save(auditLog);
    }
}
