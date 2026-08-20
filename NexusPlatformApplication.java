package com.nexus.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@SpringBootApplication
@EnableScheduling
public class NexusBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(NexusBackendApplication.class, args);
    }
}

// ==================== DATABASE ENTITIES ====================

@Entity @Table(name = "users")
class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(unique = true, nullable = false) private String email;
    @Column(unique = true, nullable = false) private String phone;
    private String fullName;
    private String passwordHash;
    private BigDecimal demoBalance = new BigDecimal("100000.0000");
    private BigDecimal realBalance = new BigDecimal("0.0000");
    private String preferredLang = "TH";
    private Boolean isActive = true;

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public BigDecimal getDemoBalance() { return demoBalance; }
    public void setDemoBalance(BigDecimal demoBalance) { this.demoBalance = demoBalance; }
    public BigDecimal getRealBalance() { return realBalance; }
    public void setRealBalance(BigDecimal realBalance) { this.realBalance = realBalance; }
}

@Entity @Table(name = "positions")
class Position {
    @Id private String id;
    private Long userId;
    private String symbol;
    private String accountType; // DEMO / REAL
    private String side; // BUY / SELL
    private BigDecimal amount;
    private Integer leverage;
    private BigDecimal entryPrice;
    private BigDecimal closePrice;
    private BigDecimal stopLoss;
    private BigDecimal takeProfit;
    private BigDecimal liquidationPrice;
    private BigDecimal pnl = BigDecimal.ZERO;
    private Boolean isAiTrade = false;
    private String status = "OPEN"; // OPEN / CLOSED / LIQUIDATED
    private LocalDateTime createdAt = LocalDateTime.now();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }
    public String getSide() { return side; }
    public void setSide(String side) { this.side = side; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Integer getLeverage() { return leverage; }
    public void setLeverage(Integer leverage) { this.leverage = leverage; }
    public BigDecimal getEntryPrice() { return entryPrice; }
    public void setEntryPrice(BigDecimal entryPrice) { this.entryPrice = entryPrice; }
    public BigDecimal getClosePrice() { return closePrice; }
    public void setClosePrice(BigDecimal closePrice) { this.closePrice = closePrice; }
    public BigDecimal getLiquidationPrice() { return liquidationPrice; }
    public void setLiquidationPrice(BigDecimal liquidationPrice) { this.liquidationPrice = liquidationPrice; }
    public BigDecimal getPnl() { return pnl; }
    public void setPnl(BigDecimal pnl) { this.pnl = pnl; }
    public Boolean getIsAiTrade() { return isAiTrade; }
    public void setIsAiTrade(Boolean isAiTrade) { this.isAiTrade = isAiTrade; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

@Entity @Table(name = "ai_logs")
class AiLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private Long userId;
    private String symbol;
    private String action; // BUY / SELL / HOLD
    private Integer confidence;
    private String indicatorDetails;
    private LocalDateTime timestamp = LocalDateTime.now();

    public void setUserId(Long userId) { this.userId = userId; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public void setAction(String action) { this.action = action; }
    public void setConfidence(Integer confidence) { this.confidence = confidence; }
    public void setIndicatorDetails(String indicatorDetails) { this.indicatorDetails = indicatorDetails; }
}

// ==================== REPOSITORIES ====================

@Repository interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}
@Repository interface PositionRepository extends JpaRepository<Position, String> {
    List<Position> findByUserIdAndStatus(Long userId, String status);
}
@Repository interface AiLogRepository extends JpaRepository<AiLog, Long> {
    List<AiLog> findTop20ByUserIdOrderByIdDesc(Long userId);
}

// ==================== AI QUANT ENGINE SERVICE ====================

@Service
class AiQuantEngine {
    private final Map<String, List<BigDecimal>> priceHistoryMap = new ConcurrentHashMap<>();

    public void updatePriceHistory(String symbol, BigDecimal price) {
        priceHistoryMap.computeIfAbsent(symbol, k -> new ArrayList<>()).add(price);
        List<BigDecimal> history = priceHistoryMap.get(symbol);
        if (history.size() > 100) {
            history.remove(0);
        }
    }

    public Map<String, Object> analyzeMarketSignal(String symbol) {
        List<BigDecimal> prices = priceHistoryMap.getOrDefault(symbol, Collections.emptyList());
        Map<String, Object> result = new HashMap<>();

        if (prices.size() < 14) {
            result.put("action", "HOLD");
            result.put("confidence", 50);
            result.put("reason", "Insufficient price data for RSI/MACD");
            return result;
        }

        // 1. คำนวณ RSI (Relative Strength Index 14 Period)
        double rsi = calculateRSI(prices, 14);
        
        // 2. คำนวณ Moving Averages (EMA 9 vs EMA 21)
        double ema9 = calculateEMA(prices, 9);
        double ema21 = calculateEMA(prices, 21);

        String action = "HOLD";
        int confidence = 60;

        if (rsi < 30 && ema9 > ema21) {
            action = "BUY";
            confidence = (int) (80 + (30 - rsi));
        } else if (rsi > 70 && ema9 < ema21) {
            action = "SELL";
            confidence = (int) (80 + (rsi - 70));
        }

        result.put("action", action);
        result.put("confidence", Math.min(confidence, 99));
        result.put("rsi", String.format("%.2f", rsi));
        result.put("ema9", String.format("%.2f", ema9));
        result.put("ema21", String.format("%.2f", ema21));
        return result;
    }

    private double calculateRSI(List<BigDecimal> prices, int period) {
        double gains = 0, losses = 0;
        for (int i = prices.size() - period; i < prices.size() - 1; i++) {
            double diff = prices.get(i + 1).doubleValue() - prices.get(i).doubleValue();
            if (diff >= 0) gains += diff;
            else losses -= diff;
        }
        if (losses == 0) return 100;
        double rs = (gains / period) / (losses / period);
        return 100 - (100 / (1 + rs));
    }

    private double calculateEMA(List<BigDecimal> prices, int period) {
        double k = 2.0 / (period + 1);
        double ema = prices.get(prices.size() - period).doubleValue();
        for (int i = prices.size() - period + 1; i < prices.size(); i++) {
            ema = (prices.get(i).doubleValue() * k) + (ema * (1 - k));
        }
        return ema;
    }
}

// ==================== CORE PLATFORM SERVICE ====================

@Service
class NexusPlatformService {
    @Autowired private UserRepository userRepo;
    @Autowired private PositionRepository posRepo;
    @Autowired private AiLogRepository aiLogRepo;
    @Autowired private AiQuantEngine aiEngine;

    @Transactional
    public Position createPosition(Long userId, String symbol, String side, BigDecimal amount, Integer leverage, BigDecimal price, String accountType, Boolean isAi) {
        User user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        BigDecimal balance = accountType.equals("DEMO") ? user.getDemoBalance() : user.getRealBalance();

        if (balance.compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient Funds");
        }

        // Deduct Balance
        if (accountType.equals("DEMO")) user.setDemoBalance(balance.subtract(amount));
        else user.setRealBalance(balance.subtract(amount));
        userRepo.save(user);

        // Liquidation Calculation
        BigDecimal liqOffset = price.divide(new BigDecimal(leverage), 4, RoundingMode.HALF_UP);
        BigDecimal liqPrice = side.equals("BUY") ? price.subtract(liqOffset) : price.add(liqOffset);

        Position pos = new Position();
        pos.setId((isAi ? "ai_" : "pos_") + System.currentTimeMillis());
        pos.setUserId(userId);
        pos.setSymbol(symbol);
        pos.setSide(side);
        pos.setAmount(amount);
        pos.setLeverage(leverage);
        pos.setEntryPrice(price);
        pos.setLiquidationPrice(liqPrice);
        pos.setAccountType(accountType);
        pos.setIsAiTrade(isAi);
        
        return posRepo.save(pos);
    }

    @Transactional
    public Position closePosition(String positionId, BigDecimal exitPrice) {
        Position pos = posRepo.findById(positionId).orElseThrow(() -> new RuntimeException("Position missing"));
        pos.setStatus("CLOSED");
        pos.setClosePrice(exitPrice);

        BigDecimal diff = exitPrice.subtract(pos.getEntryPrice());
        BigDecimal multiplier = pos.getSide().equals("BUY") ? diff : diff.negate();
        BigDecimal pnl = pos.getAmount().multiply(multiplier).multiply(new BigDecimal(pos.getLeverage())).divide(pos.getEntryPrice(), 4, RoundingMode.HALF_UP);
        pos.setPnl(pnl);

        User user = userRepo.findById(pos.getUserId()).orElseThrow();
        BigDecimal payout = pos.getAmount().add(pnl);
        if (payout.compareTo(BigDecimal.ZERO) < 0) payout = BigDecimal.ZERO;

        if (pos.getAccountType().equals("DEMO")) user.setDemoBalance(user.getDemoBalance().add(payout));
        else user.setRealBalance(user.getRealBalance().add(payout));
        
        userRepo.save(user);
        return posRepo.save(pos);
    }

    public Map<String, Object> processAiAutoTrade(Long userId, String symbol, BigDecimal currentPrice, String accountType) {
        aiEngine.updatePriceHistory(symbol, currentPrice);
        Map<String, Object> analysis = aiEngine.analyzeMarketSignal(symbol);
        String action = (String) analysis.get("action");

        AiLog log = new AiLog();
        log.setUserId(userId);
        log.setSymbol(symbol);
        log.setAction(action);
        log.setConfidence((Integer) analysis.get("confidence"));
        log.setIndicatorDetails(analysis.toString());
        aiLogRepo.save(log);

        if (!action.equals("HOLD")) {
            Position pos = createPosition(userId, symbol, action, new BigDecimal("100.00"), 20, currentPrice, accountType, true);
            analysis.put("executedPosition", pos);
        }
        return analysis;
    }

    public List<Position> getUserPositions(Long userId) { return posRepo.findByUserIdAndStatus(userId, "OPEN"); }
    public List<AiLog> getAiLogs(Long userId) { return aiLogRepo.findTop20ByUserIdOrderByIdDesc(userId); }
}

// ==================== REST CONTROLLER API ====================

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
class PlatformApiController {
    @Autowired private NexusPlatformService service;

    @PostMapping("/trading/order")
    public ResponseEntity<?> placeOrder(@RequestBody Map<String, Object> req) {
        try {
            Position pos = service.createPosition(
                Long.parseLong(req.getOrDefault("userId", "1").toString()),
                req.get("symbol").toString(),
                req.get("side").toString(),
                new BigDecimal(req.get("amount").toString()),
                Integer.parseInt(req.get("leverage").toString()),
                new BigDecimal(req.get("price").toString()),
                req.getOrDefault("accountType", "DEMO").toString(),
                Boolean.parseBoolean(req.getOrDefault("isAi", "false").toString())
            );
            return ResponseEntity.ok(pos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/trading/close/{id}")
    public ResponseEntity<?> closeOrder(@PathVariable String id, @RequestBody Map<String, Object> req) {
        BigDecimal exitPrice = new BigDecimal(req.get("exitPrice").toString());
        return ResponseEntity.ok(service.closePosition(id, exitPrice));
    }

    @PostMapping("/ai/tick")
    public ResponseEntity<?> triggerAiTick(@RequestBody Map<String, Object> req) {
        Long userId = Long.parseLong(req.getOrDefault("userId", "1").toString());
        String symbol = req.get("symbol").toString();
        BigDecimal price = new BigDecimal(req.get("price").toString());
        String accountType = req.getOrDefault("accountType", "DEMO").toString();
        return ResponseEntity.ok(service.processAiAutoTrade(userId, symbol, price, accountType));
    }

    @GetMapping("/trading/positions")
    public ResponseEntity<?> getPositions(@RequestParam(defaultValue = "1") Long userId) {
        return ResponseEntity.ok(service.getUserPositions(userId));
    }

    @GetMapping("/ai/logs")
    public ResponseEntity<?> getAiLogs(@RequestParam(defaultValue = "1") Long userId) {
        return ResponseEntity.ok(service.getAiLogs(userId));
    }
}
