package com.todongsan.marketservice.market.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.todongsan.marketservice.global.serializer.BigDecimalPlainStringSerializer;
import com.todongsan.marketservice.market.type.MarketStatus;
import com.todongsan.marketservice.market.type.SettlementStatus;
import io.swagger.v3.oas.annotations.media.Schema;
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
        @Schema(type = "string", example = "200.00")
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal totalPool,
        @Schema(type = "string", example = "5.00")
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal feeRate,
        @Schema(type = "string", example = "5.00")
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal feeAmount,
        @Schema(type = "string", example = "195.00")
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal settlementPool,
        @Schema(type = "string", example = "400.00000000")
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal winningContractQuantity,
        @Schema(type = "string", example = "0.23750000")
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal payoutPerContract,
        @Schema(type = "string", example = "0.00")
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
