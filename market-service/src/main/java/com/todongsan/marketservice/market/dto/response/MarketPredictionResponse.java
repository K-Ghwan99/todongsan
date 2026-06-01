package com.todongsan.marketservice.market.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.todongsan.marketservice.market.type.PredictionStatus;
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

    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal pointAmount;

    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal priceSnapshot;

    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal contractQuantity;

    private PredictionStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
