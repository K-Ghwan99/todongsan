package com.todongsan.marketservice.market.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.todongsan.marketservice.global.serializer.BigDecimalPlainStringSerializer;
import com.todongsan.marketservice.market.type.MarketAnswerType;
import com.todongsan.marketservice.market.type.MarketCategory;
import com.todongsan.marketservice.market.type.MarketStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record MarketInsightMarketSummaryResponse(
        Long marketId,
        String title,
        MarketCategory category,
        MarketAnswerType answerType,
        MarketStatus status,
        LocalDateTime closeAt,
        LocalDate judgeDate,
        String judgeDataSource,
        String judgeCriteria,
        Long resultOptionId,
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class)
        BigDecimal resultValue,
        String resultText,
        Long totalPredictionCount,
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class)
        BigDecimal totalPoolAmount,
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class)
        BigDecimal settlementPoolAmount,
        LocalDateTime settledAt
) {
}
