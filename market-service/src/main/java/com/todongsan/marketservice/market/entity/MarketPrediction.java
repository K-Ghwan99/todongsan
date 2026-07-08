package com.todongsan.marketservice.market.entity;

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
public class MarketPrediction {
    private Long id;

    private Long marketId;
    private Long optionId;

    private Long memberId;

    private BigDecimal pointAmount;

    private BigDecimal priceSnapshot;
    private BigDecimal contractQuantity;

    private BigDecimal expectedPayoutPerContractSnapshot;
    private BigDecimal expectedMultiplierSnapshot;

    private PredictionStatus status;

    private String pointSpendIdempotencyKey;
    private Integer attemptNo;

    private BigDecimal settledAmount;
    private BigDecimal refundAmount;

    private String failReason;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

