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
import io.swagger.v3.oas.annotations.media.Schema;
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
        @Schema(type = "string", example = "5.00")
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal feeRate,
        @Schema(type = "string", example = "5.00")
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal feeAmount,
        @Schema(type = "string", example = "195.00")
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal settlementPool,
        String judgeDataSource,
        String judgeCriteria,
        Long resultOptionId,
        @Schema(type = "string", example = "0.7500")
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal resultValue,
        String resultText,
        @Schema(type = "string", example = "200.00")
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal totalRealPoolAmount,
        @Schema(type = "string", example = "200.00")
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal totalVirtualPoolAmount,
        @Schema(type = "string", example = "400.00")
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
            @Schema(type = "string", example = "0.0000")
            @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal rangeMin,
            @Schema(type = "string", example = "0.5000")
            @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal rangeMax,
            Boolean minInclusive,
            Boolean maxInclusive,
            @Schema(type = "string", example = "0.50000000")
            @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal initialPrice,
            @Schema(type = "string", example = "0.50000000")
            @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal currentPrice,
            @Schema(type = "string", example = "0.00000000")
            @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal priceChangeRate,
            @Schema(type = "string", example = "100.00")
            @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal realPoolAmount,
            @Schema(type = "string", example = "100.00")
            @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal virtualPoolAmount,
            @Schema(type = "string", example = "200.00")
            @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal effectivePoolAmount,
            @Schema(type = "string", example = "200.00000000")
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
