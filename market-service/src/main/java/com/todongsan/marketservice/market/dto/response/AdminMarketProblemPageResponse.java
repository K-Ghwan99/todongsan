package com.todongsan.marketservice.market.dto.response;

import com.todongsan.marketservice.market.type.AdminMarketProblemStatus;
import com.todongsan.marketservice.market.type.AdminMarketProblemType;
import com.todongsan.marketservice.market.type.MarketStatus;
import java.time.LocalDateTime;
import java.util.List;

public record AdminMarketProblemPageResponse(
        List<Problem> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
    public record Problem(
            Long marketId,
            String title,
            MarketStatus marketStatus,
            AdminMarketProblemType problemType,
            AdminMarketProblemStatus problemStatus,
            long failedCount,
            long unknownCount,
            long pendingStaleCount,
            String lastErrorCode,
            String lastErrorMessage,
            LocalDateTime lastAttemptAt,
            boolean autoRecoverable,
            boolean manualCheckRequired
    ) {
    }
}
