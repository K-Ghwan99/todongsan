package com.todongsan.marketservice.market.client;

import java.math.BigDecimal;

public record MemberPointSettlementItem(
        Long predictionId,
        Long memberId,
        BigDecimal amount,
        String referenceType,
        Long referenceId,
        String reason,
        String idempotencyKey
) {
}
