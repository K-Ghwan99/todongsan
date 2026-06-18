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
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal totalPool,
        @Schema(description = "losingPool에만 부과한 수수료")
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal feeAmount,
        @Schema(description = "원금 포함 지급 가능 풀: winningPrincipalPool + rewardPool")
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal settlementPool,
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal winningContractQuantity,
        @Schema(description = "하위 호환 필드명. rewardPool의 계약당 보상액이며 원금은 포함하지 않음")
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
