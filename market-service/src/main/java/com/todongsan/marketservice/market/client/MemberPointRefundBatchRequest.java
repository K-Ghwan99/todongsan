package com.todongsan.marketservice.market.client;

import java.util.List;

public record MemberPointRefundBatchRequest(
        Long marketId,
        String refundId,
        List<MemberPointRefundItem> items
) {
}
