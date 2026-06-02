package com.todongsan.insightreputation.global.client;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class MarketInsightSummaryResponse {
    
    private MarketInfo market;
    private List<OptionStatistics> optionStatistics;
    
    @Getter
    @Builder
    public static class MarketInfo {
        private Long marketId;
        private String title;
        private String category;
        private String status;          // "SETTLED", "ACTIVE", etc.
        private LocalDateTime closeAt;
        private LocalDateTime judgeDate;
        private Long resultOptionId;
        private Integer totalPredictionCount;
        private BigDecimal totalPoolAmount;
    }
    
    @Getter
    @Builder
    public static class OptionStatistics {
        private Long optionId;
        private String optionLabel;
        private Integer predictionCount;
        private BigDecimal poolAmount;
        private Boolean isResult;
    }
}