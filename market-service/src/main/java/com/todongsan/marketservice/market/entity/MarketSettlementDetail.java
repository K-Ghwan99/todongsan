package com.todongsan.marketservice.market.entity;

import com.todongsan.marketservice.market.type.TransactionItemStatus;
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
public class MarketSettlementDetail {
    private Long id;

    private Long settlementId;
    private Long predictionId;

    private Long memberId;

    private BigDecimal originalPointAmount;
    private BigDecimal contractQuantity;

    private BigDecimal payoutPerContract;
    private BigDecimal settledAmount;
    private BigDecimal profitAmount;

    private TransactionItemStatus status;

    private String idempotencyKey;
    private String failReason;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

