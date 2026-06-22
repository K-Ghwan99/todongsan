package com.todongsan.marketservice.market.dto.response;

import java.util.List;

public record MarketCommentPageResponse(
        List<MarketCommentResponse> content,
        int number,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}
