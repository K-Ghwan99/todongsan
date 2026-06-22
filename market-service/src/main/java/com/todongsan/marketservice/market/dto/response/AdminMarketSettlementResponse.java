package com.todongsan.marketservice.market.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.todongsan.marketservice.global.serializer.BigDecimalPlainStringSerializer;
import com.todongsan.marketservice.market.type.MarketStatus;
import com.todongsan.marketservice.market.type.SettlementStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminMarketSettlementResponse(
        Long marketId,
        String marketTitle,
        MarketStatus marketStatus,
        Long settlementId,
        SettlementStatus settlementStatus,
        Long resultOptionId,
        String resultOptionText,
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal totalPool,
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal feeRate,
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal feeAmount,
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal settlementPool,
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal winningContractQuantity,
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal payoutPerContract,
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal burnedPointAmount,
        long totalDetailCount,
        long successCount,
        long failedCount,
        long unknownCount,
        long pendingCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
