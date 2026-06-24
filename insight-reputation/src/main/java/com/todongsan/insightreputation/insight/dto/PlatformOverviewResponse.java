package com.todongsan.insightreputation.insight.dto;

import java.util.Map;

public record PlatformOverviewResponse(
        PlatformStats platformStats,
        Map<String, Double> reputationDistribution,
        Double crowdIntelligenceScore,
        Map<String, Double> visitCertMethodRatio,
        AiReportStats aiReportStats
) {
    public record PlatformStats(
            long totalMembersWithReputation,
            Double avgReputationScore,
            Double avgPredictionAccuracy,
            long totalVisitCertifications,
            Integer activeMarketsCount,
            Integer activeBattlesCount
    ) {}

    public record AiReportStats(
            long totalDone,
            long totalFailed,
            long totalPending,
            Double successRate
    ) {}
}
