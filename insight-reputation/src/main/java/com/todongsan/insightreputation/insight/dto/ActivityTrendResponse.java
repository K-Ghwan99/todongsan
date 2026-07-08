package com.todongsan.insightreputation.insight.dto;

import java.time.LocalDate;
import java.util.List;

public record ActivityTrendResponse(String period, List<WeeklyData> weeklyTrend) {
    public record WeeklyData(
            LocalDate weekStart,
            long newVisitCerts,
            long aiReportsCompleted,
            Double aiReportSuccessRate,
            long predictionResultsProcessed
    ) {}
}
