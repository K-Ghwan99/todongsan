package com.todongsan.marketservice.market.dto.request;

import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConfirmMarketResultRequest {

    private Long resultOptionId;
    private BigDecimal resultValue;

    @Size(max = 255)
    private String resultText;
}
