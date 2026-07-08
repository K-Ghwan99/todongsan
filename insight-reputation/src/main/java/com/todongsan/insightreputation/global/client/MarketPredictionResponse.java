package com.todongsan.insightreputation.global.client;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class MarketPredictionResponse {
    
    private Long predictionId;
    private Long memberId;
    private Long optionId;
    private String optionLabel;
    private BigDecimal pointAmount;
    private BigDecimal priceSnapshot;
    private BigDecimal contractQuantity;
    private String status;              // "SETTLED"
    private Boolean isCorrect;
    private LocalDateTime participatedAt;
}