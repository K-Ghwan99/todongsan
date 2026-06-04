package com.todongsan.marketservice.market.service;

import com.todongsan.marketservice.market.dto.response.SettleMarketResponse;
import com.todongsan.marketservice.market.entity.MarketSettlementDetail;
import com.todongsan.marketservice.market.type.MarketStatus;
import com.todongsan.marketservice.market.type.SettlementStatus;
import java.math.BigDecimal;
import java.util.List;

record SettlementPreparation(
        Long marketId,
        Long settlementId,
        Long resultOptionId,
        BigDecimal totalPool,
        BigDecimal feeAmount,
        BigDecimal settlementPool,
        BigDecimal winningContractQuantity,
        BigDecimal payoutPerContract,
        BigDecimal burnedPointAmount,
        int winnerCount,
        int loserCount,
        List<MarketSettlementDetail> winnerDetails,
        boolean completed
) {

    SettleMarketResponse toResponse(
            MarketStatus marketStatus,
            SettlementStatus settlementStatus,
            int successCount,
            int failedCount
    ) {
        return new SettleMarketResponse(
                marketId,
                settlementId,
                resultOptionId,
                totalPool,
                feeAmount,
                settlementPool,
                winningContractQuantity,
                payoutPerContract,
                burnedPointAmount,
                winnerCount,
                loserCount,
                successCount,
                failedCount,
                marketStatus,
                settlementStatus
        );
    }
}
