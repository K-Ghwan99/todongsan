package com.todongsan.marketservice.market.entity;

import com.todongsan.marketservice.market.type.PriceHistoryEventType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MarketPriceHistory {
    private Long id;

    private Long marketId;
    private Long optionId;
    private Long predictionId;

    private BigDecimal priceBefore;
    private BigDecimal priceAfter;

    private BigDecimal realPoolBefore;
    private BigDecimal realPoolAfter;

    private BigDecimal contractQuantityBefore;
    private BigDecimal contractQuantityAfter;

    private PriceHistoryEventType eventType;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

