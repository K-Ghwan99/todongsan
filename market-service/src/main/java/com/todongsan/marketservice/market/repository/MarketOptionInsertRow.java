package com.todongsan.marketservice.market.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MarketOptionInsertRow {
    private Long marketId;
    private String optionCode;
    private String optionText;
    private Integer displayOrder;
    private BigDecimal rangeMin;
    private BigDecimal rangeMax;
    private Boolean minInclusive;
    private Boolean maxInclusive;
    private BigDecimal virtualPoolAmount;
    private BigDecimal realPoolAmount;
    private BigDecimal totalContractQuantity;
    private BigDecimal currentPrice;
    private Integer predictionCount;
    private Boolean isResult;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
