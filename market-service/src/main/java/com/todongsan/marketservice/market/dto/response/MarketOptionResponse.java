package com.todongsan.marketservice.market.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MarketOptionResponse {
    private Long optionId;
    private String content;
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal currentPrice;

    @JsonSerialize(using = ToStringSerializer.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal realPoolAmount;

    @JsonSerialize(using = ToStringSerializer.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal virtualPoolAmount;
}
