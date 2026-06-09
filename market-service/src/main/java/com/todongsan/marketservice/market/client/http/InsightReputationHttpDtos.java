package com.todongsan.marketservice.market.client.http;

import com.todongsan.marketservice.market.client.PredictionAccuracyUpdateCommand;
import java.math.BigDecimal;

public final class InsightReputationHttpDtos {

    private InsightReputationHttpDtos() {
    }

    public record PredictionAccuracyUpdateRequest(
            Long memberId,
            Long marketId,
            Long predictionId,
            Boolean isCorrect
    ) {
        public static PredictionAccuracyUpdateRequest from(PredictionAccuracyUpdateCommand command) {
            return new PredictionAccuracyUpdateRequest(
                    command.memberId(),
                    command.marketId(),
                    command.predictionId(),
                    command.isCorrect()
            );
        }
    }

    public record PredictionAccuracyUpdateResponse(
            Long memberId,
            Integer predictionCount,
            Integer predictionCorrect,
            BigDecimal predictionAccuracy
    ) {
    }
}
