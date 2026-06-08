package com.todongsan.marketservice.market.client;

public interface InsightReputationClient {

    PredictionAccuracyUpdateResult updatePredictionAccuracy(PredictionAccuracyUpdateCommand command);
}
