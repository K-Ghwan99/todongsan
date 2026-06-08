package com.todongsan.marketservice.market.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.todongsan.marketservice.global.serializer.BigDecimalPlainStringSerializer;
import com.todongsan.marketservice.market.type.PredictionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MarketInsightPredictionResponse(
        Long predictionId,
        @Schema(description = "회원 ID. 회원 프로필 정보는 제공하지 않음", example = "10")
        Long memberId,
        Long optionId,
        String optionCode,
        String optionLabel,
        @Schema(description = "참여 포인트 금액. Decimal String으로 응답", type = "string", example = "100.00")
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class)
        BigDecimal pointAmount,
        @Schema(description = "예측 참여 시점의 확정 가격 스냅샷. Decimal String으로 응답", type = "string", example = "0.42100000")
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class)
        BigDecimal priceSnapshot,
        @Schema(description = "확정 계약 수량. Decimal String으로 응답", type = "string", example = "237.52969121")
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class)
        BigDecimal contractQuantity,
        PredictionStatus status,
        Boolean isCorrect,
        LocalDateTime participatedAt
) {
}
