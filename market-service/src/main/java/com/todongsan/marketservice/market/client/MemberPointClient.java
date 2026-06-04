package com.todongsan.marketservice.market.client;

public interface MemberPointClient {
    void spend(PointSpendCommand command);

    MemberPointSettlementBatchResponse settleMarketRewards(
            String batchIdempotencyKey,
            MemberPointSettlementBatchRequest request
    );
}
