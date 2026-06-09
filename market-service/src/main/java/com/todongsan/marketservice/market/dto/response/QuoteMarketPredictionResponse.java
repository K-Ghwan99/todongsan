package com.todongsan.marketservice.market.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.todongsan.marketservice.global.serializer.BigDecimalPlainStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

public record QuoteMarketPredictionResponse(
        Long marketId,
        Long selectedOptionId,
        @Schema(description = "Quote 기준 참여 포인트 금액. Decimal String으로 응답", type = "string", example = "100.00")
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class)
        BigDecimal pointAmount,
        @Schema(description = "현재 가격. Decimal String으로 응답", type = "string", example = "0.50000000")
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class)
        BigDecimal currentPrice,
        @Schema(description = "예상 계약 수량. Quote 미리보기 값이며 확정값이 아님", type = "string", example = "200.00000000")
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class)
        BigDecimal estimatedContractQuantity,
        @Schema(description = "참여 후 예상 가격. Quote 미리보기 값이며 확정값이 아님", type = "string", example = "0.68750000")
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class)
        BigDecimal estimatedAfterPrice,
        @Schema(description = "예상 가격 영향률. Decimal String으로 응답", type = "string", example = "0.18750000")
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class)
        BigDecimal priceImpactRate,
        @Schema(description = "선택지 참여 전 유효 풀. 실제 풀과 가상 유동성을 합산한 Quote 계산용 값", type = "string", example = "100.00")
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class)
        BigDecimal selectedOptionEffectivePoolBefore,
        @Schema(description = "선택지 참여 후 유효 풀. Quote 미리보기 값", type = "string", example = "200.00")
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class)
        BigDecimal selectedOptionEffectivePoolAfter,
        @Schema(description = "참여 전 전체 유효 풀. Quote 계산용 값", type = "string", example = "200.00")
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class)
        BigDecimal totalEffectivePoolBefore,
        @Schema(description = "참여 후 전체 유효 풀. Quote 미리보기 값", type = "string", example = "300.00")
        @JsonSerialize(using = BigDecimalPlainStringSerializer.class)
        BigDecimal totalEffectivePoolAfter,
        String notice
) {
}
