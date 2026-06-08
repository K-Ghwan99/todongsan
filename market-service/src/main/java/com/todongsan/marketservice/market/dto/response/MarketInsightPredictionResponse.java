package com.todongsan.marketservice.market.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.todongsan.marketservice.global.serializer.BigDecimalPlainStringSerializer;
import com.todongsan.marketservice.market.type.PredictionStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MarketInsightPredictionResponse(
        Long predictionId,
        Long memberId,
        Long optionId,
        String optionCode,
        String optionLabel,
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class)
        BigDecimal pointAmount,
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class)
        BigDecimal priceSnapshot,
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class)
        BigDecimal contractQuantity,
        PredictionStatus status,
        Boolean isCorrect,
        LocalDateTime participatedAt
) {
}
