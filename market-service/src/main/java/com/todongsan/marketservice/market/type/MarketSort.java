package com.todongsan.marketservice.market.type;

import com.todongsan.marketservice.global.exception.CustomException;
import com.todongsan.marketservice.global.exception.errorcode.CommonErrorCode;
import java.util.Arrays;

public enum MarketSort {
    POPULAR("popular"),
    CLOSING_SOON("closingSoon"),
    LATEST("latest");

    private final String queryValue;

    MarketSort(String queryValue) {
        this.queryValue = queryValue;
    }

    public static MarketSort from(String value) {
        if (value == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(sort -> sort.queryValue.equals(value))
                .findFirst()
                .orElseThrow(() -> new CustomException(CommonErrorCode.VALIDATION_FAILED));
    }
}
