package com.todongsan.marketservice.market.controller;

import com.todongsan.marketservice.global.response.ApiResponse;
import com.todongsan.marketservice.market.dto.response.MarketInsightPredictionPageResponse;
import com.todongsan.marketservice.market.dto.response.MarketInsightSummaryResponse;
import com.todongsan.marketservice.market.service.MarketInsightService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/api/v1/markets")
public class InternalMarketInsightController {

    private final MarketInsightService marketInsightService;

    @GetMapping("/{marketId}/insight-summary")
    public ApiResponse<MarketInsightSummaryResponse> getInsightSummary(
            @PathVariable @Min(1) long marketId
    ) {
        return ApiResponse.ok(marketInsightService.getSummary(marketId));
    }

    @GetMapping("/{marketId}/insight-predictions")
    public ApiResponse<MarketInsightPredictionPageResponse> getInsightPredictions(
            @PathVariable @Min(1) long marketId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "500") @Min(1) @Max(1000) int size
    ) {
        return ApiResponse.ok(marketInsightService.getPredictions(marketId, page, size));
    }
}
