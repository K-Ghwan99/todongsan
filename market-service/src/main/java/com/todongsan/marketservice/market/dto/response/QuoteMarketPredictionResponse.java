package com.todongsan.marketservice.market.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.todongsan.marketservice.global.serializer.BigDecimalPlainStringSerializer;
import java.math.BigDecimal;

public record QuoteMarketPredictionResponse(
        Long marketId,
        Long selectedOptionId,
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class)
        BigDecimal pointAmount,
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class)
        BigDecimal currentPrice,
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class)
        BigDecimal estimatedContractQuantity,
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class)
        BigDecimal estimatedAfterPrice,
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class)
        BigDecimal priceImpactRate,
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class)
        BigDecimal selectedOptionEffectivePoolBefore,
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class)
        BigDecimal selectedOptionEffectivePoolAfter,
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class)
        BigDecimal totalEffectivePoolBefore,
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class)
        BigDecimal totalEffectivePoolAfter,
        String notice
) {
}
