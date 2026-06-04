package com.todongsan.marketservice.market.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.todongsan.marketservice.global.serializer.BigDecimalPlainStringSerializer;
import com.todongsan.marketservice.market.type.MarketStatus;
import com.todongsan.marketservice.market.type.SettlementStatus;
import java.math.BigDecimal;

public record SettleMarketResponse(
        Long marketId,
        Long settlementId,
        Long resultOptionId,
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal totalPool,
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal feeAmount,
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal settlementPool,
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal winningContractQuantity,
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal payoutPerContract,
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal burnedPointAmount,
        int winnerCount,
        int loserCount,
        int successCount,
        int failedCount,
        MarketStatus marketStatus,
        SettlementStatus settlementStatus
) {
}
