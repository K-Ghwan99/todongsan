package com.todongsan.marketservice.market.client;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class FakeMemberPointClient implements MemberPointClient {

    @Override
    public void spend(PointSpendCommand command) {
        // Phase 1 keeps the transaction boundary while Member-Point HTTP integration is deferred.
    }

    @Override
    public MemberPointSettlementBatchResponse settleMarketRewards(
            String batchIdempotencyKey,
            MemberPointSettlementBatchRequest request
    ) {
        List<MemberPointSettlementItemResult> results = request.items().stream()
                .map(item -> new MemberPointSettlementItemResult(
                        item.predictionId(),
                        item.memberId(),
                        MemberPointSettlementItemStatus.PROCESSED,
                        null,
                        item.amount(),
                        null
                ))
                .toList();
        return new MemberPointSettlementBatchResponse(request.marketId(), results);
    }
}
