package com.todongsan.memberpointservice.point.entity;

public enum PointReferenceType {

    BATTLE,             // Battle 도메인 (reference_id = battleId)
    MARKET_PREDICTION,  // Market Prediction 도메인 (reference_id = predictionId)
    INSIGHT_REPORT      // Insight Report 도메인 (reference_id = reportId)
}
