package com.todongsan.marketservice.market.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.todongsan.marketservice.global.serializer.BigDecimalPlainStringSerializer;
import com.todongsan.marketservice.market.type.MarketAnswerType;
import com.todongsan.marketservice.market.type.MarketCategory;
import com.todongsan.marketservice.market.type.MarketDisplayStatus;
import com.todongsan.marketservice.market.type.MarketMetricUnit;
import com.todongsan.marketservice.market.type.MarketPriceModel;
import com.todongsan.marketservice.market.type.MarketStatus;
import com.todongsan.marketservice.market.type.MarketVoidReasonType;
import com.todongsan.marketservice.market.type.RefundStatus;
import com.todongsan.marketservice.market.type.SettlementStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AdminMarketDetailResponse(
        Long marketId,
        String title,
        String description,
        MarketCategory category,
        MarketAnswerType answerType,
        MarketMetricUnit metricUnit,
        MarketStatus status,
        MarketDisplayStatus displayStatus,
        boolean canPredict,
        MarketPriceModel priceModel,
        LocalDateTime closeAt,
        LocalDate judgeDate,
        LocalDateTime settleDueAt,
        LocalDateTime settledAt,
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal feeRate,
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal feeAmount,
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal settlementPool,
        String judgeDataSource,
        String judgeCriteria,
        Long resultOptionId,
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal resultValue,
        String resultText,
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal totalRealPoolAmount,
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal totalVirtualPoolAmount,
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal totalEffectivePoolAmount,
        long totalPredictionCount,
        List<AdminMarketOption> options,
        AdminSettlementSummary settlementSummary,
        AdminRefundSummary refundSummary
) {
    public record AdminMarketOption(
            Long optionId,
            String optionCode,
            String content,
            Integer displayOrder,
            @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal rangeMin,
            @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal rangeMax,
            Boolean minInclusive,
            Boolean maxInclusive,
            @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal initialPrice,
            @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal currentPrice,
            @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal priceChangeRate,
            @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal realPoolAmount,
            @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal virtualPoolAmount,
            @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal effectivePoolAmount,
            @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal totalContractQuantity,
            Integer predictionCount,
            Boolean isResult
    ) {
    }

    public record AdminSettlementSummary(
            Long settlementId,
            SettlementStatus status,
            long totalDetailCount,
            long successCount,
            long failedCount,
            long unknownCount,
            long pendingCount,
            LocalDateTime updatedAt
    ) {
    }

    public record AdminRefundSummary(
            Long voidId,
            MarketVoidReasonType reasonType,
            RefundStatus refundStatus,
            boolean refundRequired,
            long totalDetailCount,
            long successCount,
            long failedCount,
            long unknownCount,
            long pendingCount,
            LocalDateTime updatedAt
    ) {
    }
}
