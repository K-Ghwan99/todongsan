package com.todongsan.marketservice.market.client;

import java.util.List;

public record MemberPointSettlementBatchResponse(
        Long marketId,
        List<MemberPointSettlementItemResult> results
) {
}
