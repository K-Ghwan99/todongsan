package com.todongsan.marketservice.market.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.todongsan.marketservice.global.serializer.BigDecimalPlainStringSerializer;
import com.todongsan.marketservice.market.type.TransactionItemStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AdminMarketSettlementDetailPageResponse(
        List<Detail> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
    public record Detail(
            Long settlementDetailId,
            Long settlementId,
            Long predictionId,
            Long memberId,
            TransactionItemStatus status,
            @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal originalPointAmount,
            @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal contractQuantity,
            @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal payoutPerContract,
            @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal settledAmount,
            @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal profitAmount,
            String idempotencyKey,
            String failReason,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }
}
