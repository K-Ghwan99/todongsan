package com.todongsan.marketservice.market.repository;

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
public class MarketInsightPredictionRow {
    private Long predictionId;
    private Long memberId;
    private Long optionId;
    private String optionCode;
    private String optionLabel;
    private BigDecimal pointAmount;
    private BigDecimal priceSnapshot;
    private BigDecimal contractQuantity;
    private PredictionStatus status;
    private Boolean isCorrect;
    private LocalDateTime participatedAt;
}
