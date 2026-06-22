package com.todongsan.marketservice.market.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.todongsan.marketservice.global.serializer.BigDecimalPlainStringSerializer;
import com.todongsan.marketservice.market.type.TransactionItemStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminSettlementDetailResponse(
        Long settlementDetailId,
        Long settlementId,
        Long predictionId,
        Long memberId,
        Long selectedOptionId,
        @Schema(type = "string", example = "100.00")
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal pointAmount,
        @Schema(type = "string", example = "200.00000000")
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal contractQuantity,
        @Schema(type = "string", example = "195.00")
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal settledAmount,
        @Schema(type = "string", example = "95.00")
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class) BigDecimal profitAmount,
        TransactionItemStatus status,
        String failureReason,
        String idempotencyKey,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
