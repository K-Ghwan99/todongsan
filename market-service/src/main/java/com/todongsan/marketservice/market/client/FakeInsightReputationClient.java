package com.todongsan.marketservice.market.client;

import java.math.BigDecimal;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "client.insight.mode",
        havingValue = "fake",
        matchIfMissing = true
)
public class FakeInsightReputationClient implements InsightReputationClient {

    @Override
    public PredictionAccuracyUpdateResult updatePredictionAccuracy(PredictionAccuracyUpdateCommand command) {
        return new PredictionAccuracyUpdateResult(
                command.memberId(),
                0,
                0,
                BigDecimal.ZERO
        );
    }
}
