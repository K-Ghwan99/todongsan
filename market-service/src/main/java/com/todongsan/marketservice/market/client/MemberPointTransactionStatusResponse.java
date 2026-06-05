package com.todongsan.marketservice.market.client;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MemberPointTransactionStatusResponse(
        String idempotencyKey,
        MemberPointTransactionStatus status,
        Long memberId,
        String type,
        BigDecimal amount,
        String referenceType,
        Long referenceId,
        Boolean requestHashMatched,
        BigDecimal balanceSnapshot,
        LocalDateTime createdAt,
        String errorCode
) {
}
