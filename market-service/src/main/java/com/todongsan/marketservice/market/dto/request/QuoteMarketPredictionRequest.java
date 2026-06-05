package com.todongsan.marketservice.market.dto.request;

import jakarta.validation.constraints.NotNull;

public record QuoteMarketPredictionRequest(
        @NotNull Long marketOptionId,
        String pointAmount
) {
}
