package com.todongsan.marketservice.market.type;

import com.todongsan.marketservice.global.exception.CustomException;
import com.todongsan.marketservice.global.exception.errorcode.CommonErrorCode;

public enum AdminMarketProblemType {
    ALL,
    PREDICTION_RECONCILE,
    SETTLEMENT,
    REFUND,
    REPUTATION;

    public static AdminMarketProblemType from(String value) {
        if (value == null || value.isBlank()) {
            return ALL;
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CustomException(CommonErrorCode.VALIDATION_FAILED);
        }
    }
}
