package com.todongsan.marketservice.market.client;

public record PredictionAccuracyUpdateCommand(
        Long memberId,
        Long marketId,
        Long predictionId,
        Boolean isCorrect
) {
}
