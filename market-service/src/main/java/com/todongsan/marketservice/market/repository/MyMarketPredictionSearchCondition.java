package com.todongsan.marketservice.market.repository;

import com.todongsan.marketservice.market.type.MarketStatus;
import com.todongsan.marketservice.market.type.PredictionStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MyMarketPredictionSearchCondition {
    private Long memberId;
    private int offset;
    private int limit;
    private LocalDateTime now;
    private boolean marketDisplayStatusFilterApplied;
    private boolean includeDisplayActive;
    private boolean includeClosedByTime;
    private List<MarketStatus> marketStatusesForDisplayFilter;
    private List<PredictionStatus> predictionStatuses;
}
