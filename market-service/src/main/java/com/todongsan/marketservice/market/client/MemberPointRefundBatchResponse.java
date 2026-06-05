package com.todongsan.marketservice.market.client;

import java.util.List;

public record MemberPointRefundBatchResponse(
        Long marketId,
        List<MemberPointRefundItemResult> results
) {
}
