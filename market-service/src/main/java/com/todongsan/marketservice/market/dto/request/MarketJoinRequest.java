package com.todongsan.marketservice.market.dto.request;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MarketJoinRequest {
    @NotNull
    private Long marketOptionId;

    @NotNull
    @DecimalMin("10.00")
    @DecimalMax("500.00")
    private BigDecimal pointAmount;
}
