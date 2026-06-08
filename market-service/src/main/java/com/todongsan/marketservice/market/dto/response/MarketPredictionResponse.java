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
}
