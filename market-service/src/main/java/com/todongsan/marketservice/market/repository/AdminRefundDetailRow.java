package com.todongsan.marketservice.market.repository;

import com.todongsan.marketservice.market.type.TransactionItemStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminRefundDetailRow {
    private Long refundDetailId;
    private Long voidId;
    private Long predictionId;
    private Long memberId;
    private BigDecimal pointAmount;
    private BigDecimal refundAmount;
    private TransactionItemStatus status;
    private String failureReason;
    private String idempotencyKey;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
