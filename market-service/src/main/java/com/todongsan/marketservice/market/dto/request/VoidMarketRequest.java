package com.todongsan.marketservice.market.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VoidMarketRequest(
        @NotBlank
        @Size(max = 50)
        String reasonCode,

        @NotBlank
        String reason
) {
}
