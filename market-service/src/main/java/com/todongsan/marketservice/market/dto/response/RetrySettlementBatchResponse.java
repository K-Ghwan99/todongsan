package com.todongsan.marketservice.market.dto.response;

public record RetrySettlementBatchResponse(
        int requestedLimit,
        int scannedMarketCount,
        int retriedMarketCount,
        int settledMarketCount,
        int stillInProgressCount,
        int skippedCount,
        int failedCount
) {
}
