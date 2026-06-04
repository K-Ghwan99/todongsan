package com.todongsan.marketservice.market.client;

import java.util.List;

public record MemberPointSettlementBatchRequest(
        Long marketId,
        String settlementId,
        List<MemberPointSettlementItem> items
) {
}
