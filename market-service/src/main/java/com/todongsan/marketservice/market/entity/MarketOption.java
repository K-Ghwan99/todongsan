package com.todongsan.marketservice.market.entity;

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
public class MarketOption {
    private Long id;
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

