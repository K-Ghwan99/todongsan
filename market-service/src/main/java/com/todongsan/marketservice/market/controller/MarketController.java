package com.todongsan.marketservice.market.controller;

import com.todongsan.marketservice.global.response.ApiResponse;
import com.todongsan.marketservice.market.dto.request.CreatePredictionRequest;
import com.todongsan.marketservice.market.dto.response.CreatePredictionResponse;
import com.todongsan.marketservice.market.dto.response.MarketDetailResponse;
import com.todongsan.marketservice.market.dto.response.MarketListResponse;
import com.todongsan.marketservice.market.dto.response.MarketPredictionResponse;
import com.todongsan.marketservice.market.dto.response.MarketPriceHistoryResponse;
import com.todongsan.marketservice.market.service.MarketPredictionQueryService;
import com.todongsan.marketservice.market.service.MarketPredictionService;
import com.todongsan.marketservice.market.service.MarketService;
import com.todongsan.marketservice.market.type.MarketStatus;
import com.todongsan.marketservice.market.type.PredictionStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/markets")
public class MarketController {

    private final MarketService marketService;
    private final MarketPredictionQueryService marketPredictionQueryService;
    private final MarketPredictionService marketPredictionService;

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

    @PostMapping("/{marketId}/predictions")
    public ApiResponse<CreatePredictionResponse> createPrediction(
            @PathVariable @Min(1) long marketId,
            @RequestHeader(value = "X-Member-Id", required = false) Long memberId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody CreatePredictionRequest request
    ) {
        CreatePredictionResponse response = marketPredictionService.createPrediction(
                marketId,
                memberId,
                idempotencyKey,
                request
        );
        if (response.getStatus() == PredictionStatus.POINT_UNKNOWN) {
            return ApiResponse.ok(response, "예측 참여 처리 상태를 확인 중입니다.");
        }
        return ApiResponse.ok(response);
    }

    @GetMapping("/{marketId}/predictions/me")
    public ApiResponse<MarketPredictionResponse> getMyPrediction(
            @PathVariable @Min(1) long marketId,
            @RequestHeader(value = "X-Member-Id", required = false) Long memberId
    ) {
        return ApiResponse.ok(marketPredictionQueryService.getMyPrediction(marketId, memberId));
    }
}
