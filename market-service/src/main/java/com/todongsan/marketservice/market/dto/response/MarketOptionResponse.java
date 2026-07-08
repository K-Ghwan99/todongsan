package com.todongsan.marketservice.market.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(description = "현재 가격. Decimal String으로 응답", type = "string", example = "0.50000000")
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal currentPrice;

    @Schema(description = "실제 참여 풀. Decimal String으로 응답", type = "string", example = "0.00")
    @JsonSerialize(using = ToStringSerializer.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal realPoolAmount;

    @Schema(description = "생성 시 설정되는 가상 유동성. 생성 후 수정 불가", type = "string", example = "100.00")
    @JsonSerialize(using = ToStringSerializer.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal virtualPoolAmount;
}
