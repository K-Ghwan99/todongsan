package com.todongsan.marketservice.market.controller;

import com.todongsan.marketservice.global.response.ApiResponse;
import com.todongsan.marketservice.market.dto.response.MarketDetailResponse;
import com.todongsan.marketservice.market.dto.response.MarketListResponse;
import com.todongsan.marketservice.market.dto.response.MarketPriceHistoryResponse;
import com.todongsan.marketservice.market.service.MarketService;
import com.todongsan.marketservice.market.type.MarketStatus;
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
@RequestMapping("/api/v1/markets")
public class MarketController {

    private final MarketService marketService;

    @GetMapping
    public ApiResponse<MarketListResponse> getMarkets(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) MarketStatus status,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.ok(marketService.getMarkets(page, size, status, keyword));
    }

    @GetMapping("/{marketId}")
    public ApiResponse<MarketDetailResponse> getMarket(
            @PathVariable @Min(1) long marketId
    ) {
        return ApiResponse.ok(marketService.getMarket(marketId));
    }

    @GetMapping("/{marketId}/price-history")
    public ApiResponse<MarketPriceHistoryResponse> getPriceHistory(
            @PathVariable @Min(1) long marketId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size,
            @RequestParam(required = false) @Min(1) Long optionId
    ) {
        return ApiResponse.ok(marketService.getPriceHistory(marketId, page, size, optionId));
    }
}
