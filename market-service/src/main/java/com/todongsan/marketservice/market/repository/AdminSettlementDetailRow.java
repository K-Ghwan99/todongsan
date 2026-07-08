package com.todongsan.marketservice.market.repository;

import com.todongsan.marketservice.market.type.TransactionItemStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminSettlementDetailRow {
    private Long settlementDetailId;
    private Long settlementId;
    private Long predictionId;
    private Long memberId;
    private Long selectedOptionId;
    private BigDecimal pointAmount;
    private BigDecimal contractQuantity;
    private BigDecimal settledAmount;
    private BigDecimal profitAmount;
    private TransactionItemStatus status;
    private String failureReason;
    private String idempotencyKey;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
