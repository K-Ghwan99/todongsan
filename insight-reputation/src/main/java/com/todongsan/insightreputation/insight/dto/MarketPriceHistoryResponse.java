package com.todongsan.insightreputation.insight.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketPriceHistoryResponse {

    private String regionSido;
    private String regionSigu;
    private String dataType;
    private List<PricePoint> priceHistory;
    private List<OptionDistribution> latestPredictionDistribution;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PricePoint {
        private LocalDate referenceDate;
        private BigDecimal value;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionDistribution {
        private String optionLabel;
        private double ratio;
        private Boolean isResult;
    }
}
