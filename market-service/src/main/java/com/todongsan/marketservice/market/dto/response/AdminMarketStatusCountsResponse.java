package com.todongsan.marketservice.market.dto.response;

public record AdminMarketStatusCountsResponse(
        long total,
        long pending,
        long active,
        long closedByTime,
        long closed,
        long dataPending,
        long settlementInProgress,
        long settled,
        long voided,
        long problemMarketCount
) {
}
