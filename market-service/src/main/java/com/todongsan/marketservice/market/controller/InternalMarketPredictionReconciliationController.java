package com.todongsan.marketservice.market.controller;

import com.todongsan.marketservice.global.response.ApiResponse;
import com.todongsan.marketservice.market.dto.response.ReconcilePredictionSpendResponse;
import com.todongsan.marketservice.market.service.PredictionSpendReconciliationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/internal/markets/predictions")
public class InternalMarketPredictionReconciliationController {

    private final PredictionSpendReconciliationService predictionSpendReconciliationService;

    @PostMapping("/reconcile")
    public ApiResponse<ReconcilePredictionSpendResponse> reconcilePredictionSpends(
            @RequestParam(defaultValue = "100") @Min(1) @Max(500) int limit
    ) {
        return ApiResponse.ok(predictionSpendReconciliationService.reconcile(limit));
    }
}
