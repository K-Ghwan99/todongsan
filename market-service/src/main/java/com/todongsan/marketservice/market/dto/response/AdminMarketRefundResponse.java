package com.todongsan.marketservice.market.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.todongsan.marketservice.global.serializer.BigDecimalPlainStringSerializer;
import com.todongsan.marketservice.market.type.MarketStatus;
import com.todongsan.marketservice.market.type.MarketVoidReasonType;
import com.todongsan.marketservice.market.type.RefundStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminMarketRefundResponse(
        Long marketId,
        String marketTitle,
        MarketStatus marketStatus,
        Long voidId,
        MarketVoidReasonType reasonType,
        String reasonDetail,
        RefundStatus refundStatus,
        boolean refundRequired,
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal totalRefundAmount,
        long totalDetailCount,
        long successCount,
        long failedCount,
        long unknownCount,
        long pendingCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
