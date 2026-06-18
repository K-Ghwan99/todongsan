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
        BigDecimal refundAmount,
        @Schema(description = "현재 기준 예상 정산금. 내 선택지가 정답일 경우의 추정값", type = "string", example = "11.87")
        @JsonSerialize(using = ToStringSerializer.class)
        BigDecimal estimatedPayoutIfWin,
        @Schema(description = "현재 기준 예상 손익", type = "string", example = "1.87")
        @JsonSerialize(using = ToStringSerializer.class)
        BigDecimal estimatedProfitIfWin,
        @Schema(description = "현재 기준 예상 수익률(%)", type = "string", example = "18.70")
        @JsonSerialize(using = ToStringSerializer.class)
        BigDecimal estimatedProfitRateIfWin,
        @Schema(description = "현재 기준 계약당 예상 지급 포인트", type = "string", example = "0.47500000")
        @JsonSerialize(using = ToStringSerializer.class)
        BigDecimal currentPayoutPerContract,
        @Schema(description = "현재 CONFIRMED 예측 포인트 총합", type = "string", example = "100.00")
        @JsonSerialize(using = ToStringSerializer.class)
        BigDecimal estimateBaseTotalPool,
        @Schema(description = "현재 기준 수수료 차감 후 예상 정산 풀", type = "string", example = "95.00")
        @JsonSerialize(using = ToStringSerializer.class)
        BigDecimal estimateBaseSettlementPool,
        @Schema(description = "선택 option의 현재 CONFIRMED 계약 수량 합", type = "string", example = "200.00000000")
        @JsonSerialize(using = ToStringSerializer.class)
        BigDecimal estimateBaseOptionContractQuantity
) {
}
