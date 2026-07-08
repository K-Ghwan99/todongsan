package com.todongsan.marketservice.market.dto.response;

public record MarketCommentDeleteResponse(
        Long commentId,
        boolean deleted
) {
}
