package com.todongsan.marketservice.market.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.todongsan.marketservice.market.type.MarketStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

public record ConfirmMarketResultResponse(
        Long marketId,
        Long resultOptionId,
        @Schema(type = "string", example = "0.7500")
        @JsonSerialize(using = ToStringSerializer.class) BigDecimal resultValue,
        String resultText,
        MarketStatus status
) {
}
