package com.todongsan.marketservice.market.service;

import com.todongsan.marketservice.global.exception.CustomException;
import com.todongsan.marketservice.global.exception.errorcode.MarketErrorCode;
import java.math.BigDecimal;

final class PredictionPointAmountValidator {

    private static final BigDecimal MIN_POINT_AMOUNT = new BigDecimal("10.00");
    private static final BigDecimal MAX_POINT_AMOUNT = new BigDecimal("500.00");
    private static final int MONEY_SCALE = 2;

    private PredictionPointAmountValidator() {
    }

    static BigDecimal parseAndValidate(String pointAmount) {
        if (pointAmount == null || pointAmount.isBlank()) {
            throw invalidBetAmount();
        }
        try {
            return validate(new BigDecimal(pointAmount.trim()));
        } catch (NumberFormatException e) {
            throw invalidBetAmount();
        }
    }

    static BigDecimal validate(BigDecimal pointAmount) {
        if (pointAmount == null
                || pointAmount.scale() > MONEY_SCALE
                || pointAmount.compareTo(MIN_POINT_AMOUNT) < 0
                || pointAmount.compareTo(MAX_POINT_AMOUNT) > 0) {
            throw invalidBetAmount();
        }
        return pointAmount.setScale(MONEY_SCALE);
    }

    private static CustomException invalidBetAmount() {
        return new CustomException(MarketErrorCode.MARKET_INVALID_BET_AMOUNT);
    }
}
