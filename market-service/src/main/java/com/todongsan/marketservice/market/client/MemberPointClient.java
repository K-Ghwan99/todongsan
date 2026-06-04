package com.todongsan.marketservice.market.client;

public interface MemberPointClient {
    void spend(PointSpendCommand command);

    MemberPointTransactionStatusResponse getTransactionStatus(String idempotencyKey);

    MemberPointSettlementBatchResponse settleMarketRewards(
            String batchIdempotencyKey,
            MemberPointSettlementBatchRequest request
    );
}
