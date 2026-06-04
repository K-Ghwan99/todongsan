package com.todongsan.marketservice.market.dto.response;

public record ReconcilePredictionSpendResponse(
        int requestedLimit,
        int scannedCount,
        int processedCount,
        int confirmedCount,
        int failedCount,
        int notFoundCount,
        int unknownCount,
        int skippedCount
) {
}
