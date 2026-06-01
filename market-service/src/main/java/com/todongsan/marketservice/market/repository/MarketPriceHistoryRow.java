package com.todongsan.marketservice.market.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MarketPriceHistoryRow {
    private Long historyId;
    private Long optionId;
    private BigDecimal price;
    private BigDecimal realPoolAmount;
    private BigDecimal virtualPoolAmount;
    private BigDecimal contractQuantity;
    private LocalDateTime createdAt;
}
