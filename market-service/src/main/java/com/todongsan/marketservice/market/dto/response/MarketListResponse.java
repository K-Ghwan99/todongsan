package com.todongsan.marketservice.market.dto.response;

import com.todongsan.marketservice.market.type.MarketStatus;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MarketListResponse {
    private List<MarketSummary> content;
    private Integer page;
    private Integer size;
    private Long totalElements;
    private Integer totalPages;
    private Boolean last;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarketSummary {
        private Long marketId;
        private String title;
        private MarketStatus status;
        private LocalDateTime closeAt;
        @Schema(description = "실제 참여 포인트 총합. Decimal String으로 응답", type = "string", example = "1000.00")
        @JsonSerialize(using = ToStringSerializer.class)
        private BigDecimal totalPoolAmount;
        private List<MarketOptionResponse> options;
    }
}
