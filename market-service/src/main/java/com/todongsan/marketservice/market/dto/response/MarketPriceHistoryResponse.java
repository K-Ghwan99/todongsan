package com.todongsan.marketservice.market.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.todongsan.marketservice.global.serializer.BigDecimalPlainStringSerializer;
import com.todongsan.marketservice.market.type.PriceHistoryEventType;
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
        private Long marketId;
        private Long optionId;
        private String optionContent;
        private Long predictionId;
        private PriceHistoryEventType eventType;

        @JsonSerialize(using = BigDecimalPlainStringSerializer.class)
        private BigDecimal priceBefore;

        @JsonSerialize(using = BigDecimalPlainStringSerializer.class)
        private BigDecimal priceAfter;

        @JsonSerialize(using = BigDecimalPlainStringSerializer.class)
        private BigDecimal priceChangeRate;

        @JsonSerialize(using = BigDecimalPlainStringSerializer.class)
        private BigDecimal realPoolBefore;

        @JsonSerialize(using = BigDecimalPlainStringSerializer.class)
        private BigDecimal realPoolAfter;

        @JsonSerialize(using = BigDecimalPlainStringSerializer.class)
        private BigDecimal virtualPoolAmount;

        @JsonSerialize(using = BigDecimalPlainStringSerializer.class)
        private BigDecimal contractQuantityBefore;

        @JsonSerialize(using = BigDecimalPlainStringSerializer.class)
        private BigDecimal contractQuantityAfter;

        private LocalDateTime createdAt;
    }
}
