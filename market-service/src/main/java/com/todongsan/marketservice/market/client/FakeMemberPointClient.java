package com.todongsan.marketservice.market.client;

import org.springframework.stereotype.Component;

@Component
public class FakeMemberPointClient implements MemberPointClient {

    @Override
    public void spend(PointSpendCommand command) {
        // Phase 1 keeps the transaction boundary while Member-Point HTTP integration is deferred.
    }
}
