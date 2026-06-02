package com.todongsan.marketservice.market.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.todongsan.marketservice.market.type.PredictionStatus;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CreatePredictionResponse {
    private final Long predictionId;
    private final Long marketId;
    private final Long selectedOptionId;

    @JsonSerialize(using = ToStringSerializer.class)
    private final BigDecimal pointAmount;

    @JsonSerialize(using = ToStringSerializer.class)
    private final BigDecimal priceSnapshot;

    @JsonSerialize(using = ToStringSerializer.class)
    private final BigDecimal contractQuantity;

    private final PredictionStatus status;
}
