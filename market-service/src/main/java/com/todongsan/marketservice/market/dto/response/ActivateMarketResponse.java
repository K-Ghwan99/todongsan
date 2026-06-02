package com.todongsan.marketservice.market.dto.response;

import com.todongsan.marketservice.market.type.MarketStatus;

public record ActivateMarketResponse(
        Long marketId,
        MarketStatus status
) {
}
