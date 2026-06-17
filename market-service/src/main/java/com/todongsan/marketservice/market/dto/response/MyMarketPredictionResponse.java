package com.todongsan.marketservice.market.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.todongsan.marketservice.market.type.MarketDisplayStatus;
import com.todongsan.marketservice.market.type.MarketStatus;
import com.todongsan.marketservice.market.type.PredictionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MyMarketPredictionResponse(
        Long predictionId,
        Long marketId,
        String marketTitle,
        MarketStatus marketStatus,
        MarketDisplayStatus marketDisplayStatus,
        Boolean canPredict,
        Long selectedOptionId,
        String selectedOptionContent,
        @Schema(description = "참여 포인트 금액. Decimal String으로 응답", type = "string", example = "100.00")
        @JsonSerialize(using = ToStringSerializer.class)
        BigDecimal pointAmount,
        @Schema(description = "예측 참여 확정 시점 가격. Decimal String으로 응답", type = "string", example = "0.50000000")
        @JsonSerialize(using = ToStringSerializer.class)
        BigDecimal priceSnapshot,
        @Schema(description = "확정 계약 수량. Decimal String으로 응답", type = "string", example = "200.00000000")
        @JsonSerialize(using = ToStringSerializer.class)
        BigDecimal contractQuantity,
        PredictionStatus predictionStatus,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime closeAt,
        @Schema(description = "정산 지급 금액. 정산 전이면 null. Decimal String으로 응답", type = "string", example = "185.30")
        @JsonSerialize(using = ToStringSerializer.class)
        BigDecimal settledAmount,
        @Schema(description = "환불 금액. 환불 전이면 null. Decimal String으로 응답", type = "string", example = "100.00")
        @JsonSerialize(using = ToStringSerializer.class)
        BigDecimal refundAmount
) {
}
