package com.todongsan.marketservice.market.dto.response;

import com.todongsan.marketservice.market.type.MarketStatus;

public record VoidMarketResponse(
        Long marketId,
        Long voidId,
        MarketStatus status,
        boolean refundRequired,
        int refundablePredictionCount,
        String reasonCode,
        String reason
) {
}
