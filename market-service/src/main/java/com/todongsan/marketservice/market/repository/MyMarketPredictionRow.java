package com.todongsan.marketservice.market.repository;

import com.todongsan.marketservice.market.type.MarketStatus;
import com.todongsan.marketservice.market.type.PredictionStatus;
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
public class MyMarketPredictionRow {
    private Long predictionId;
    private Long marketId;
    private String marketTitle;
    private MarketStatus marketStatus;
    private Long selectedOptionId;
    private String selectedOptionContent;
    private BigDecimal pointAmount;
    private BigDecimal priceSnapshot;
    private BigDecimal contractQuantity;
    private PredictionStatus predictionStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime closeAt;
    private BigDecimal settledAmount;
    private BigDecimal refundAmount;
    private BigDecimal feeRate;
    private BigDecimal estimateBaseTotalPool;
    private BigDecimal estimateBaseOptionPointAmount;
    private BigDecimal estimateBaseOptionContractQuantity;
}
