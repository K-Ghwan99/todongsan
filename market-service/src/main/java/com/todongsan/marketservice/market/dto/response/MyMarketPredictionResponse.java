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
        @Schema(description = "정답 선택지 내 rewardPool 분배 가중치로 사용하는 확정 계약 수량. 고정 지급권이 아님", type = "string", example = "200.00000000")
        @JsonSerialize(using = ToStringSerializer.class)
        BigDecimal contractQuantity,
        PredictionStatus predictionStatus,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime closeAt,
        @Schema(description = "실제 정산 지급액. winner는 원금 + reward share, loser는 0.00", type = "string", example = "195.00")
        @JsonSerialize(using = ToStringSerializer.class)
        BigDecimal settledAmount,
        @Schema(description = "환불 금액. 환불 전이면 null. Decimal String으로 응답", type = "string", example = "100.00")
        @JsonSerialize(using = ToStringSerializer.class)
        BigDecimal refundAmount,
        @Schema(description = "현재 기준 예상 정산금. 내 선택지가 정답일 경우 원금과 패자 reward share의 합", type = "string", example = "15.93")
        @JsonSerialize(using = ToStringSerializer.class)
        BigDecimal estimatedPayoutIfWin,
        @Schema(description = "현재 기준 예상 손익", type = "string", example = "1.87")
        @JsonSerialize(using = ToStringSerializer.class)
        BigDecimal estimatedProfitIfWin,
        @Schema(description = "현재 기준 예상 수익률(%)", type = "string", example = "18.70")
        @JsonSerialize(using = ToStringSerializer.class)
        BigDecimal estimatedProfitRateIfWin,
        @Schema(description = "패자 rewardPool의 현재 기준 계약당 보상액. 원금은 포함하지 않음", type = "string", example = "0.23750000")
        @JsonSerialize(using = ToStringSerializer.class)
        BigDecimal currentPayoutPerContract,
        @Schema(description = "현재 CONFIRMED 예측 포인트 총합", type = "string", example = "100.00")
        @JsonSerialize(using = ToStringSerializer.class)
        BigDecimal estimateBaseTotalPool,
        @Schema(description = "내 선택지가 정답일 때 다른 option의 실제 참여 풀에서 수수료를 제외한 rewardPool", type = "string", example = "47.50")
        @JsonSerialize(using = ToStringSerializer.class)
        BigDecimal estimateBaseSettlementPool,
        @Schema(description = "선택 option의 현재 CONFIRMED 계약 수량 합", type = "string", example = "200.00000000")
        @JsonSerialize(using = ToStringSerializer.class)
        BigDecimal estimateBaseOptionContractQuantity
) {
}
