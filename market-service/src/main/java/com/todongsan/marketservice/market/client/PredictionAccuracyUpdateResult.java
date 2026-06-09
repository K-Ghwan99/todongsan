package com.todongsan.marketservice.market.client;

import java.math.BigDecimal;

public record PredictionAccuracyUpdateResult(
        Long memberId,
        Integer predictionCount,
        Integer predictionCorrect,
        BigDecimal predictionAccuracy
) {
}
