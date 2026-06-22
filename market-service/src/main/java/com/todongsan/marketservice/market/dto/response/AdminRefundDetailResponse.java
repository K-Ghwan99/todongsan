package com.todongsan.marketservice.market.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.todongsan.marketservice.global.serializer.BigDecimalPlainStringSerializer;
import com.todongsan.marketservice.market.type.TransactionItemStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminRefundDetailResponse(
        Long refundDetailId,
        Long voidId,
        Long predictionId,
        Long memberId,
        @Schema(type = "string", example = "100.00")
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal pointAmount,
        @Schema(type = "string", example = "100.00")
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal refundAmount,
        TransactionItemStatus status,
        String failureReason,
        String idempotencyKey,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
