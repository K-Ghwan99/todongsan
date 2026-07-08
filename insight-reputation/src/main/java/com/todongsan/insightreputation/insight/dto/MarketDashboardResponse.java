package com.todongsan.insightreputation.insight.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
public class MarketDashboardResponse {

    private Long marketId;
    private String title;
    private String regionSido;
    private String regionSigu;
    private PriceHistorySection priceHistory;
    private List<PredictionOption> predictionDistribution;
    private PriceVsPredictionOverlay priceVsPredictionOverlay;
    private ParticipantStats participantStats;
    private VisitCertStats visitCertStats;

    @Getter
    @AllArgsConstructor
    public static class PriceHistorySection {
        private String dataType;
        private List<PriceRecord> records;
        private String trendDirection;
        private Double changeRate;
    }

    @Getter
    @AllArgsConstructor
    public static class PriceRecord {
        private LocalDate referenceDate;
        private BigDecimal value;
    }

    @Getter
    @AllArgsConstructor
    public static class PredictionOption {
        private String optionLabel;
        private Double ratio;
        private boolean isResult;
    }

    @Getter
    @AllArgsConstructor
    public static class PriceVsPredictionOverlay {
        private String priceTrendDirection;
        private Double priceChangePct;
        private Boolean crowdPredictedCorrectly;
        private String majorityOption;
    }

    @Getter
    @AllArgsConstructor
    public static class ParticipantStats {
        private int totalParticipants;
        private long totalPoolAmount;
        private Map<String, Double> genderDistribution;
        private Map<String, Double> ageGroupDistribution;
        private Double residenceMatchRatio;
    }

    @Getter
    @AllArgsConstructor
    public static class VisitCertStats {
        private long certifiedVisitorCount;
        private long gpsCertCount;
        private long commentCertCount;
    }
}
