package com.todongsan.marketservice.market.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateMarketOptionRequest {

    @NotBlank
    @Size(max = 20)
    private String optionCode;

    @NotBlank
    @Size(max = 100)
    private String optionText;

    private Integer displayOrder;
    private BigDecimal rangeMin;
    private BigDecimal rangeMax;
    private Boolean minInclusive;
    private Boolean maxInclusive;

    private BigDecimal virtualPoolAmount;
}
