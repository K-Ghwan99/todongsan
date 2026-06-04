package com.todongsan.marketservice.market.client;

import java.math.BigDecimal;

public record MemberPointRefundItemResult(
        Long predictionId,
        Long memberId,
        MemberPointRefundItemStatus status,
        String errorCode,
        BigDecimal amount,
        BigDecimal balanceSnapshot
) {
}
