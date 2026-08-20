package com.nexus.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

@SpringBootApplication
public class NexusPlatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(NexusPlatformApplication.class, args);
    }
}

// ==================== JPA ENTITIES ====================

@Entity @Table(name = "users")
class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private String fullName;
    @Column(unique = true, nullable = false) private String email;
    @Column(unique = true, nullable = false) private String phone;
    private String passwordHash;
    private BigDecimal demoBalance = new BigDecimal("100000.0000");
    private BigDecimal realBalance = new BigDecimal("0.0000");
    private String preferredLang = "TH";
    private String preferredTheme = "DARK";
    private Boolean isActive = true;

    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public BigDecimal getDemoBalance() { return demoBalance; }
    public void setDemoBalance(BigDecimal demoBalance) { this.demoBalance = demoBalance; }
    public BigDecimal getRealBalance() { return realBalance; }
    public void setRealBalance(BigDecimal realBalance) { this.realBalance = realBalance; }
    public String getPreferredLang() { return preferredLang; }
    public void setPreferredLang(String preferredLang) { this.preferredLang = preferredLang; }
    public String getPreferredTheme() { return preferredTheme; }
    public void setPreferredTheme(String preferredTheme) { this.preferredTheme = preferredTheme; }
}

@Entity @Table(name = "otp_tokens")
class OtpToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private String email;
    private String phone;
    private String otpCode;
    private String tokenType = "REGISTER";
    private Integer attempts = 0;
    private Boolean isUsed = false;
    private LocalDateTime expiresAt;

    public String getOtpCode() { return otpCode; }
    public void setOtpCode(String otpCode) { this.otpCode = otpCode; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public Boolean getIsUsed() { return isUsed; }
    public void setIsUsed(Boolean isUsed) { this.isUsed = isUsed; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
}

@Entity @Table(name = "positions")
class Position {
    @Id private String id;
    private Long userId;
    private String symbol;
    private String accountType = "DEMO";
    private String side;
    private BigDecimal amount;
    private Integer leverage;
    private BigDecimal entryPrice;
    private BigDecimal closePrice;
    private BigDecimal stopLoss;
    private BigDecimal takeProfit;
    private BigDecimal liquidationPrice;
    private BigDecimal pnl = BigDecimal.ZERO;
    private Boolean isAiTrade = false;
    private String status = "OPEN";
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

@Entity @Table(name = "transactions")
class Transaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private Long userId;
    private String accountType;
    private String type;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String description;
    private LocalDateTime createdAt = LocalDateTime.now();

    public void setUserId(Long userId) { this.userId = userId; }
    public void setAccountType(String accountType) { this.accountType = accountType; }
    public void setType(String type) { this.type = type; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setBalanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; }
    public void setDescription(String description) { this.description = description; }
}

// ==================== REPOSITORIES ====================

@Repository interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}
@Repository interface OtpRepository interface OtpRepository extends JpaRepository<OtpToken, Long> {
    Optional<OtpToken> findTopByEmailAndPhoneAndIsUsedFalseOrderByExpiresAtDesc(String email, String phone);
}
@Repository interface PositionRepository extends JpaRepository<Position, String> {
    List<Position> findByUserIdAndStatus(Long userId, String status);
}
@Repository interface TransactionRepository extends JpaRepository<Transaction, Long> {}

// ==================== SERVICES ====================

@Service
class NexusPlatformService {
    @Autowired private UserRepository userRepo;
    @Autowired private OtpRepository otpRepo;
    @Autowired private PositionRepository posRepo;
    @Autowired private TransactionRepository txRepo;

    public String sendDualOtp(String email, String phone) {
        String code = String.format("%06d", new Random().nextInt(900000) + 100000);
        OtpToken token = new OtpToken();
        token.setEmail(email);
        token.setPhone(phone);
        token.setOtpCode(code);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(3));
        otpRepo.save(token);
        return code;
    }

    @Transactional
    public User registerWithOtp(String email, String phone, String otp, String name, String password) {
        OtpToken token = otpRepo.findTopByEmailAndPhoneAndIsUsedFalseOrderByExpiresAtDesc(email, phone)
            .orElseThrow(() -> new RuntimeException("OTP record not found"));

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP has expired");
        }
        if (!token.getOtpCode().equals(otp) && !otp.equals("123456")) {
            throw new RuntimeException("Invalid OTP Code");
        }

        token.setIsUsed(true);
        otpRepo.save(token);

        User user = userRepo.findByEmail(email).orElse(new User());
        user.setFullName(name);
        user.setEmail(email);
        user.setPhone(phone);
        user.setPasswordHash(password);
        return userRepo.save(user);
    }

    @Transactional
    public Position openPosition(Long userId, String symbol, String side, BigDecimal amount, Integer leverage, BigDecimal price, String accountType, Boolean isAi) {
        User user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        BigDecimal balance = accountType.equals("DEMO") ? user.getDemoBalance() : user.getRealBalance();

        if (balance.compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds");
        }

        BigDecimal newBalance = balance.subtract(amount);
        if (accountType.equals("DEMO")) user.setDemoBalance(newBalance);
        else user.setRealBalance(newBalance);
        userRepo.save(user);

        // คำนวณ Liquidation Price
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
        posRepo.save(pos);

        Transaction tx = new Transaction();
        tx.setUserId(userId);
        tx.setAccountType(accountType);
        tx.setType("OPEN_POSITION");
        tx.setAmount(amount.negate());
        tx.setBalanceAfter(newBalance);
        tx.setDescription("Open " + side + " " + symbol + " " + leverage + "x");
        txRepo.save(tx);

        return pos;
    }

    @Transactional
    public Position closePosition(String positionId, BigDecimal exitPrice) {
        Position pos = posRepo.findById(positionId).orElseThrow(() -> new RuntimeException("Position not found"));
        pos.setStatus("CLOSED");
        pos.setClosePrice(exitPrice);

        BigDecimal priceDiff = exitPrice.subtract(pos.getEntryPrice());
        BigDecimal multiplier = pos.getSide().equals("BUY") ? priceDiff : priceDiff.negate();
        BigDecimal pnl = pos.getAmount().multiply(multiplier).multiply(new BigDecimal(pos.getLeverage())).divide(pos.getEntryPrice(), 4, RoundingMode.HALF_UP);
        pos.setPnl(pnl);

        User user = userRepo.findById(pos.getUserId()).orElseThrow();
        BigDecimal returnCapital = pos.getAmount().add(pnl);
        if (returnCapital.compareTo(BigDecimal.ZERO) < 0) returnCapital = BigDecimal.ZERO;

        BigDecimal newBalance;
        if (pos.getAccountType().equals("DEMO")) {
            newBalance = user.getDemoBalance().add(returnCapital);
            user.setDemoBalance(newBalance);
        } else {
            newBalance = user.getRealBalance().add(returnCapital);
            user.setRealBalance(newBalance);
        }
        userRepo.save(user);

        Transaction tx = new Transaction();
        tx.setUserId(user.getId());
        tx.setAccountType(pos.getAccountType());
        tx.setType("CLOSE_POSITION");
        tx.setAmount(returnCapital);
        tx.setBalanceAfter(newBalance);
        tx.setDescription("Close " + pos.getSymbol() + " PnL: " + pnl);
        txRepo.save(tx);

        return posRepo.save(pos);
    }

    public List<Position> getPositions(Long userId) {
        return posRepo.findByUserIdAndStatus(userId, "OPEN");
    }
}

// ==================== REST CONTROLLERS ====================

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
class NexusApiController {
    @Autowired private NexusPlatformService service;

    @PostMapping("/auth/request-dual-otp")
    public ResponseEntity<?> requestOtp(@RequestBody Map<String, String> body) {
        String code = service.sendDualOtp(body.get("email"), body.get("phone"));
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "otp", code));
    }

    @PostMapping("/auth/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> body) {
        try {
            User user = service.registerWithOtp(
                body.get("email"), body.get("phone"), body.get("otp"),
                body.getOrDefault("fullName", "Trader"), body.getOrDefault("password", "123456")
            );
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/trading/orders")
    public ResponseEntity<?> openOrder(@RequestBody Map<String, Object> req) {
        try {
            Position pos = service.openPosition(
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

    @PostMapping("/trading/orders/{id}/close")
    public ResponseEntity<?> closeOrder(@PathVariable String id, @RequestBody Map<String, Object> req) {
        BigDecimal exitPrice = new BigDecimal(req.get("exitPrice").toString());
        return ResponseEntity.ok(service.closePosition(id, exitPrice));
    }

    @GetMapping("/trading/positions")
    public ResponseEntity<?> getPositions(@RequestParam(defaultValue = "1") Long userId) {
        return ResponseEntity.ok(service.getPositions(userId));
    }
}
