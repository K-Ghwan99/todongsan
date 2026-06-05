package com.todongsan.marketservice.market.client;

import java.math.BigDecimal;

public record MemberPointSettlementItemResult(
        Long predictionId,
        Long memberId,
        MemberPointSettlementItemStatus status,
        String errorCode,
        BigDecimal amount,
        BigDecimal balanceSnapshot
) {
}
