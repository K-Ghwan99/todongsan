package com.todongsan.marketservice.market.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.todongsan.marketservice.global.serializer.BigDecimalPlainStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

public record MarketInsightOptionStatisticsResponse(
        Long optionId,
        String optionCode,
        String optionLabel,
        @Schema(description = "선택지 범위 최소값. Decimal String으로 응답", type = "string", example = "0.0000")
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class)
        BigDecimal rangeMin,
        @Schema(description = "선택지 범위 최대값. Decimal String으로 응답", type = "string", example = "0.5000")
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class)
        BigDecimal rangeMax,
        Boolean minInclusive,
        Boolean maxInclusive,
        Long predictionCount,
        Long participantCount,
        @Schema(description = "해당 선택지의 실제 참여 포인트 합계. virtualPoolAmount를 포함하지 않음", type = "string", example = "300.00")
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class)
        BigDecimal poolAmount,
        @Schema(description = "최종 현재 가격. Decimal String으로 응답", type = "string", example = "0.42100000")
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class)
        BigDecimal finalPrice,
        @Schema(description = "총 계약 수량. Decimal String으로 응답", type = "string", example = "712.50000000")
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class)
        BigDecimal totalContractQuantity,
        Boolean isResult
) {
}
