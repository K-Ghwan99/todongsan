package com.todongsan.marketservice.market.repository;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MarketInsightOptionStatisticsRow {
    private Long optionId;
    private String optionCode;
    private String optionLabel;
    private BigDecimal rangeMin;
    private BigDecimal rangeMax;
    private Boolean minInclusive;
    private Boolean maxInclusive;
    private Long predictionCount;
    private Long participantCount;
    private BigDecimal poolAmount;
    private BigDecimal finalPrice;
    private BigDecimal totalContractQuantity;
    private Boolean isResult;
}
