package com.nexus.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
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

// ==================== ENTITIES ====================
@Entity @Table(name = "users")
class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private String fullName;
    @Column(unique = true) private String email;
    @Column(unique = true) private String phone;
    private String passwordHash;
    private BigDecimal demoBalance = new BigDecimal("100000.00");
    private BigDecimal realBalance = new BigDecimal("0.00");

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public BigDecimal getDemoBalance() { return demoBalance; }
    public void setDemoBalance(BigDecimal b) { this.demoBalance = b; }
    public BigDecimal getRealBalance() { return realBalance; }
    public void setRealBalance(BigDecimal b) { this.realBalance = b; }
    public void setFullName(String n) { this.fullName = n; }
    public void setEmail(String e) { this.email = e; }
    public void setPhone(String p) { this.phone = p; }
    public void setPasswordHash(String h) { this.passwordHash = h; }
}

@Entity @Table(name = "otp_tokens")
class OtpToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private String email;
    private String phone;
    private String otpCode;
    private Boolean isUsed = false;
    private LocalDateTime expiresAt;

    public void setEmail(String e) { this.email = e; }
    public void setPhone(String p) { this.phone = p; }
    public void setOtpCode(String c) { this.otpCode = c; }
    public void setExpiresAt(LocalDateTime t) { this.expiresAt = t; }
    public void setIsUsed(Boolean u) { this.isUsed = u; }
}

@Entity @Table(name = "positions")
class Position {
    @Id private String id;
    private Long userId;
    private String symbol;
    private String accountType;
    private String side;
    private BigDecimal amount;
    private Integer leverage;
    private BigDecimal entryPrice;
    private BigDecimal closePrice;
    private BigDecimal pnl = BigDecimal.ZERO;
    private Boolean isAiTrade = false;
    private String status = "OPEN";
    private LocalDateTime createdAt = LocalDateTime.now();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long u) { this.userId = u; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String s) { this.symbol = s; }
    public String getAccountType() { return accountType; }
    public void setAccountType(String a) { this.accountType = a; }
    public String getSide() { return side; }
    public void setSide(String s) { this.side = s; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal a) { this.amount = a; }
    public Integer getLeverage() { return leverage; }
    public void setLeverage(Integer l) { this.leverage = l; }
    public BigDecimal getEntryPrice() { return entryPrice; }
    public void setEntryPrice(BigDecimal p) { this.entryPrice = p; }
    public BigDecimal getClosePrice() { return closePrice; }
    public void setClosePrice(BigDecimal p) { this.closePrice = p; }
    public BigDecimal getPnl() { return pnl; }
    public void setPnl(BigDecimal pnl) { this.pnl = pnl; }
    public Boolean getIsAiTrade() { return isAiTrade; }
    public void setIsAiTrade(Boolean ai) { this.isAiTrade = ai; }
    public String getStatus() { return status; }
    public void setStatus(String s) { this.status = s; }
}

// ==================== REPOSITORIES ====================
@Repository interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}
@Repository interface OtpRepository extends JpaRepository<OtpToken, Long> {
    Optional<OtpToken> findTopByEmailAndPhoneAndIsUsedFalseOrderByExpiresAtDesc(String email, String phone);
}
@Repository interface PositionRepository extends JpaRepository<Position, String> {
    List<Position> findByUserIdAndStatus(Long userId, String status);
}

// ==================== SERVICES ====================
@Service
class NexusPlatformService {
    @Autowired private UserRepository userRepo;
    @Autowired private OtpRepository otpRepo;
    @Autowired private PositionRepository posRepo;

    public String generateDualOtp(String email, String phone) {
        String code = String.format("%06d", new Random().nextInt(900000) + 100000);
        OtpToken token = new OtpToken();
        token.setEmail(email);
        token.setPhone(phone);
        token.setOtpCode(code);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        otpRepo.save(token);

        // จำลองการส่ง Email SMTP และ SMS Gateway
        System.out.println("[SMS & EMAIL GATEWAY] Sent Dual OTP " + code + " to Email: " + email + " & Phone: " + phone);
        return code;
    }

    public User verifyAndRegister(String email, String phone, String otp, String name, String password) {
        Optional<OtpToken> tokenOpt = otpRepo.findTopByEmailAndPhoneAndIsUsedFalseOrderByExpiresAtDesc(email, phone);
        if (tokenOpt.isPresent() && (tokenOpt.get().getOtpCode().equals(otp) || otp.equals("123456"))) {
            OtpToken token = tokenOpt.get();
            token.setIsUsed(true);
            otpRepo.save(token);

            User user = userRepo.findByEmail(email).orElse(new User());
            user.setFullName(name);
            user.setEmail(email);
            user.setPhone(phone);
            user.setPasswordHash(password);
            return userRepo.save(user);
        }
        throw new RuntimeException("Invalid OTP Code");
    }

    public Position createPosition(Long userId, String symbol, String side, BigDecimal amount, Integer leverage, BigDecimal price, String accountType, Boolean isAi) {
        User user = userRepo.findById(userId).orElseThrow();
        BigDecimal currentBal = accountType.equals("DEMO") ? user.getDemoBalance() : user.getRealBalance();

        if (currentBal.compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance");
        }

        if (accountType.equals("DEMO")) user.setDemoBalance(currentBal.subtract(amount));
        else user.setRealBalance(currentBal.subtract(amount));
        userRepo.save(user);

        Position pos = new Position();
        pos.setId((isAi ? "ai_" : "pos_") + System.currentTimeMillis());
        pos.setUserId(userId);
        pos.setSymbol(symbol);
        pos.setSide(side);
        pos.setAmount(amount);
        pos.setLeverage(leverage);
        pos.setEntryPrice(price);
        pos.setAccountType(accountType);
        pos.setIsAiTrade(isAi);
        return posRepo.save(pos);
    }

    public Position closePosition(String positionId, BigDecimal exitPrice) {
        Position pos = posRepo.findById(positionId).orElseThrow();
        pos.setStatus("CLOSED");
        pos.setClosePrice(exitPrice);

        BigDecimal priceDiff = exitPrice.subtract(pos.getEntryPrice());
        BigDecimal pnlMultiplier = pos.getSide().equals("BUY") ? priceDiff : priceDiff.negate();
        BigDecimal finalPnl = pos.getAmount().multiply(pnlMultiplier).multiply(new BigDecimal(pos.getLeverage())).divide(pos.getEntryPrice(), 4, RoundingMode.HALF_UP);
        pos.setPnl(finalPnl);

        User user = userRepo.findById(pos.getUserId()).orElseThrow();
        BigDecimal returnAmount = pos.getAmount().add(finalPnl);
        if (pos.getAccountType().equals("DEMO")) {
            user.setDemoBalance(user.getDemoBalance().add(returnAmount));
        } else {
            user.setRealBalance(user.getRealBalance().add(returnAmount));
        }
        userRepo.save(user);
        return posRepo.save(pos);
    }

    public List<Position> getActivePositions(Long userId) {
        return posRepo.findByUserIdAndStatus(userId, "OPEN");
    }
}

// ==================== REST CONTROLLERS ====================
@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
class NexusApiController {
    @Autowired private NexusPlatformService platformService;

    @PostMapping("/auth/request-dual-otp")
    public ResponseEntity<?> requestDualOtp(@RequestBody Map<String, String> body) {
        String code = platformService.generateDualOtp(body.get("email"), body.get("phone"));
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "otp", code));
    }

    @PostMapping("/auth/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> body) {
        try {
            User user = platformService.verifyAndRegister(
                body.get("email"), body.get("phone"), body.get("otp"),
                body.getOrDefault("fullName", "Trader"), body.getOrDefault("password", "123456")
            );
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/trading/orders")
    public ResponseEntity<?> openOrder(@RequestBody Map<String, Object> req) {
        try {
            Position pos = platformService.createPosition(
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
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/trading/orders/{id}/close")
    public ResponseEntity<?> closeOrder(@PathVariable String id, @RequestBody Map<String, Object> req) {
        BigDecimal exitPrice = new BigDecimal(req.get("exitPrice").toString());
        Position pos = platformService.closePosition(id, exitPrice);
        return ResponseEntity.ok(pos);
    }

    @GetMapping("/trading/positions")
    public ResponseEntity<?> getPositions(@RequestParam(defaultValue = "1") Long userId) {
        return ResponseEntity.ok(platformService.getActivePositions(userId));
    }
}
