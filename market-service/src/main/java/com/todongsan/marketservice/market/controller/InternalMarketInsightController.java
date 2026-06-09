package com.todongsan.marketservice.market.controller;

import com.todongsan.marketservice.global.response.ApiResponse;
import com.todongsan.marketservice.market.dto.response.MarketInsightPredictionPageResponse;
import com.todongsan.marketservice.market.dto.response.MarketInsightSummaryResponse;
import com.todongsan.marketservice.market.service.MarketInsightService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Market Insight Internal API", description = "Insight-Reputation Service가 직접 호출하는 Market 원본 데이터 조회 내부 API")
@io.swagger.v3.oas.annotations.responses.ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "VALIDATION_FAILED"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "MARKET_NOT_FOUND"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "MARKET_INVALID_STATUS / MARKET_NO_PREDICTIONS")
})
public class InternalMarketInsightController {

    private final MarketInsightService marketInsightService;

    @GetMapping("/{marketId}/insight-summary")
    @Operation(
            summary = "Insight용 Market 요약/선택지 집계 조회",
            description = """
                    Gateway를 통하지 않는 서비스 간 내부 API이다.
                    Market Service는 AI 분석 결과를 생성하지 않고 SETTLED Market의 원본 참여 데이터만 읽기 용도로 제공한다.
                    회원 프로필 정보는 제공하지 않으며, Insight-Reputation Service가 별도로 조회한다.
                    """
    )
    public ApiResponse<MarketInsightSummaryResponse> getInsightSummary(
            @Parameter(description = "Market ID. SETTLED 상태만 조회 가능", example = "1")
            @PathVariable @Min(1) long marketId
    ) {
        return ApiResponse.ok(marketInsightService.getSummary(marketId));
    }

    @GetMapping("/{marketId}/insight-predictions")
    @Operation(
            summary = "Insight용 Prediction 페이지 조회",
            description = """
                    Gateway를 통하지 않는 서비스 간 내부 API이다.
                    SETTLED Market의 SETTLED Prediction만 createdAt ASC, id ASC 순서로 조회한다.
                    page 기본값은 0, size 기본값은 500, size 최대값은 1000이며 잘못된 page/size는 VALIDATION_FAILED를 반환한다.
                    """
    )
    public ApiResponse<MarketInsightPredictionPageResponse> getInsightPredictions(
            @Parameter(description = "Market ID. SETTLED 상태만 조회 가능", example = "1")
            @PathVariable @Min(1) long marketId,
            @Parameter(description = "페이지 번호. 0부터 시작", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "페이지 크기. 기본 500, 최대 1000", example = "500")
            @RequestParam(defaultValue = "500") @Min(1) @Max(1000) int size
    ) {
        return ApiResponse.ok(marketInsightService.getPredictions(marketId, page, size));
    }
}
