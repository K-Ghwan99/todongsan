package com.todongsan.marketservice.market.dto.request;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePredictionRequest {
    private Long marketOptionId;
    private BigDecimal pointAmount;
}
