package com.todongsan.marketservice.market.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.todongsan.marketservice.global.serializer.BigDecimalPlainStringSerializer;
import java.math.BigDecimal;

public record MarketInsightOptionStatisticsResponse(
        Long optionId,
        String optionCode,
        String optionLabel,
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class)
        BigDecimal rangeMin,
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class)
        BigDecimal rangeMax,
        Boolean minInclusive,
        Boolean maxInclusive,
        Long predictionCount,
        Long participantCount,
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class)
        BigDecimal poolAmount,
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class)
        BigDecimal finalPrice,
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class)
        BigDecimal totalContractQuantity,
        Boolean isResult
) {
}
