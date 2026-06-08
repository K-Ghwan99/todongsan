package com.todongsan.marketservice.market.dto.response;

import java.util.List;

public record MarketInsightSummaryResponse(
        MarketInsightMarketSummaryResponse market,
        List<MarketInsightOptionStatisticsResponse> optionStatistics
) {
}
