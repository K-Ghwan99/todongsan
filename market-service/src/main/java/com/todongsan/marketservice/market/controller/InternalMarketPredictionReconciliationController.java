package com.todongsan.marketservice.market.controller;

import com.todongsan.marketservice.global.response.ApiResponse;
import com.todongsan.marketservice.market.dto.response.ReconcilePredictionSpendResponse;
import com.todongsan.marketservice.market.service.PredictionSpendReconciliationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Market Internal API", description = "Gateway를 통하지 않는 Market Service 내부 운영 API")
@io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "VALIDATION_FAILED")
})
public class InternalMarketPredictionReconciliationController {

    private final PredictionSpendReconciliationService predictionSpendReconciliationService;

    @PostMapping("/reconcile")
    @Operation(
            summary = "Prediction 포인트 차감 결과 대사",
            description = "POINT_PENDING/POINT_UNKNOWN Prediction의 Member-Point 처리 결과를 대사하는 내부 API이다. 기존 경로 /api/v1/internal/...을 유지한다."
    )
    public ApiResponse<ReconcilePredictionSpendResponse> reconcilePredictionSpends(
            @Parameter(description = "한 번에 대사할 최대 Prediction 수. 최대 500", example = "100")
            @RequestParam(defaultValue = "100") @Min(1) @Max(500) int limit
    ) {
        return ApiResponse.ok(predictionSpendReconciliationService.reconcile(limit));
    }
}
