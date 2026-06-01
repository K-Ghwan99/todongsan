package com.todongsan.marketservice.market.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
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
public class MarketPriceHistoryResponse {
    private List<PriceHistory> content;
    private Integer page;
    private Integer size;
    private Long totalElements;
    private Integer totalPages;
    private Boolean last;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriceHistory {
        private Long historyId;
        private Long optionId;

        @JsonSerialize(using = ToStringSerializer.class)
        private BigDecimal price;

        @JsonSerialize(using = ToStringSerializer.class)
        private BigDecimal realPoolAmount;

        @JsonSerialize(using = ToStringSerializer.class)
        private BigDecimal virtualPoolAmount;

        @JsonSerialize(using = ToStringSerializer.class)
        private BigDecimal contractQuantity;

        private LocalDateTime createdAt;
    }
}
