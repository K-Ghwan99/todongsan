package com.todongsan.marketservice.market.entity;

import com.todongsan.marketservice.market.type.SettlementStatus;
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
public class MarketSettlement {
    private Long id;

    private Long marketId;
    private Long resultOptionId;

    private BigDecimal totalPool;
    private BigDecimal feeRate;
    private BigDecimal feeAmount;
    private BigDecimal settlementPool;

    private BigDecimal winningContractQuantity;
    private BigDecimal payoutPerContract;

    private BigDecimal burnedPointAmount;

    private SettlementStatus status;

    private Long settledBy;
    private LocalDateTime settledAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

