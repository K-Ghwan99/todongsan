package com.todongsan.marketservice.market.client;

import java.math.BigDecimal;

public record PointSpendCommand(
        Long memberId,
        String type,
        BigDecimal amount,
        String referenceType,
        Long referenceId,
        String idempotencyKey
) {
}
