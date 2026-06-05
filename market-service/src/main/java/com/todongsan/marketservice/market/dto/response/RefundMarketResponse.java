package com.todongsan.marketservice.market.dto.response;

import com.todongsan.marketservice.market.type.MarketStatus;
import com.todongsan.marketservice.market.type.RefundStatus;

public record RefundMarketResponse(
        Long marketId,
        Long voidId,
        int refundTargetCount,
        int successCount,
        int failedCount,
        int unknownCount,
        MarketStatus marketStatus,
        RefundStatus refundStatus
) {
}
