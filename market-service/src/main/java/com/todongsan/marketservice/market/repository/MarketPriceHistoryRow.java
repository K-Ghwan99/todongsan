package com.todongsan.marketservice.market.repository;

import com.todongsan.marketservice.market.type.PriceHistoryEventType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MarketPriceHistoryRow {
    private Long historyId;
    private Long marketId;
    private Long optionId;
    private String optionContent;
    private Long predictionId;
    private PriceHistoryEventType eventType;
    private BigDecimal priceBefore;
    private BigDecimal priceAfter;
    private BigDecimal realPoolBefore;
    private BigDecimal realPoolAfter;
    private BigDecimal virtualPoolAmount;
    private BigDecimal contractQuantityBefore;
    private BigDecimal contractQuantityAfter;
    private LocalDateTime createdAt;
}
