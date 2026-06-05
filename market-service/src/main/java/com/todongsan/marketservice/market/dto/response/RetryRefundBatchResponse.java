package com.todongsan.marketservice.market.dto.response;

public record RetryRefundBatchResponse(
        int requestedLimit,
        int scannedMarketCount,
        int retriedMarketCount,
        int completedMarketCount,
        int stillInProgressCount,
        int skippedCount,
        int failedCount
) {
}
