package com.todongsan.marketservice.market.dto.response;

import java.time.LocalDateTime;

public record MarketCommentResponse(
        Long commentId,
        Long marketId,
        Long memberId,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
