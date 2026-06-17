package com.todongsan.marketservice.market.dto.response;

import java.util.List;

public record MyMarketPredictionPageResponse(
        List<MyMarketPredictionResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
}
