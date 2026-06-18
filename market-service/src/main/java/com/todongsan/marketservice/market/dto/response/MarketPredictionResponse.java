package com.todongsan.marketservice.market.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.todongsan.marketservice.market.type.PredictionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MarketPredictionResponse {
    private Long predictionId;
    private Long marketId;
    private Long selectedOptionId;

    @Schema(description = "참여 포인트 금액. Decimal String으로 응답", type = "string", example = "100.00")
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal pointAmount;

    @Schema(description = "예측 참여 시점에 확정된 가격 스냅샷. Decimal String으로 응답", type = "string", example = "0.50000000")
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal priceSnapshot;

    @Schema(description = "확정 계약 수량. Decimal String으로 응답", type = "string", example = "200.00000000")
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal contractQuantity;

    private PredictionStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Schema(description = "현재 기준 예상 정산금. 내 선택지가 정답일 경우의 추정값", type = "string", example = "11.87")
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal estimatedPayoutIfWin;

    @Schema(description = "현재 기준 예상 손익", type = "string", example = "1.87")
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal estimatedProfitIfWin;

    @Schema(description = "현재 기준 예상 수익률(%)", type = "string", example = "18.70")
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal estimatedProfitRateIfWin;

    @Schema(description = "현재 기준 계약당 예상 지급 포인트", type = "string", example = "0.47500000")
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal currentPayoutPerContract;

    @Schema(description = "현재 CONFIRMED 예측 포인트 총합", type = "string", example = "100.00")
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal estimateBaseTotalPool;

    @Schema(description = "현재 기준 수수료 차감 후 예상 정산 풀", type = "string", example = "95.00")
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal estimateBaseSettlementPool;

    @Schema(description = "선택 option의 현재 CONFIRMED 계약 수량 합", type = "string", example = "200.00000000")
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal estimateBaseOptionContractQuantity;
}
