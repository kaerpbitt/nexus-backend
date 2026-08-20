package com.financial.engine.core;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class FinancialEngine {

    public static final int MONEY_SCALE = 2;
    public static final int PRICE_SCALE = 4;
    public static final int ASSET_SCALE = 8;
    
    public static final RoundingMode MONEY_ROUNDING = RoundingMode.HALF_EVEN;
    public static final RoundingMode ASSET_ROUNDING = RoundingMode.DOWN;

    private FinancialEngine() {
        // Utility class constraint
    }

    public static BigDecimal createMoney(String value) {
        if (value == null || value.trim().isEmpty()) {
            return new BigDecimal("0.00").setScale(MONEY_SCALE, MONEY_ROUNDING);
        }
        return new BigDecimal(value).setScale(MONEY_SCALE, MONEY_ROUNDING);
    }

    public static BigDecimal createMoney(double value) {
        return BigDecimal.valueOf(value).setScale(MONEY_SCALE, MONEY_ROUNDING);
    }

    public static BigDecimal createAssetQuantity(String value) {
        if (value == null || value.trim().isEmpty()) {
            return new BigDecimal("0.00000000").setScale(ASSET_SCALE, ASSET_ROUNDING);
        }
        return new BigDecimal(value).setScale(ASSET_SCALE, ASSET_ROUNDING);
    }

    public static BigDecimal createPrice(String value) {
        if (value == null || value.trim().isEmpty()) {
            return new BigDecimal("0.0000").setScale(PRICE_SCALE, RoundingMode.HALF_UP);
        }
        return new BigDecimal(value).setScale(PRICE_SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculateRequiredMargin(BigDecimal volume, BigDecimal price, BigDecimal leverage) {
        if (volume == null || price == null || leverage == null || leverage.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid input for margin calculation");
        }
        
        // Notional Value = Volume * Price
        BigDecimal notionalValue = volume.multiply(price);
        
        // Required Margin = Notional Value / Leverage
        return notionalValue.divide(leverage, MONEY_SCALE, MONEY_ROUNDING);
    }

    public static boolean hasSufficientMargin(BigDecimal freeMargin, BigDecimal requiredMargin) {
        if (freeMargin == null || requiredMargin == null) {
            return false;
        }
        return freeMargin.compareTo(requiredMargin) >= 0;
    }
}
