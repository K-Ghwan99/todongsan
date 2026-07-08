package com.todongsan.marketservice.market.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.todongsan.marketservice.global.serializer.BigDecimalPlainStringSerializer;
import com.todongsan.marketservice.market.type.MarketStatus;
import com.todongsan.marketservice.market.type.SettlementStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

public record SettleMarketResponse(
        Long marketId,
        Long settlementId,
        Long resultOptionId,
        @Schema(type = "string", example = "200.00")
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal totalPool,
        @Schema(description = "losingPool에만 부과한 수수료", type = "string", example = "5.00")
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal feeAmount,
        @Schema(description = "원금 포함 지급 가능 풀: winningPrincipalPool + rewardPool", type = "string", example = "195.00")
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal settlementPool,
        @Schema(type = "string", example = "400.00000000")
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal winningContractQuantity,
        @Schema(description = "rewardPool의 계약당 보상액이며 원금은 포함하지 않음", type = "string", example = "0.23750000")
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal payoutPerContract,
        @Schema(type = "string", example = "0.00")
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal burnedPointAmount,
        int winnerCount,
        int loserCount,
        int successCount,
        int failedCount,
        MarketStatus marketStatus,
        SettlementStatus settlementStatus
) {
}
