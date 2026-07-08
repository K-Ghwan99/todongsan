package com.todongsan.insightreputation.insight.controller;

import com.todongsan.insightreputation.global.exception.CustomException;
import com.todongsan.insightreputation.global.exception.errorcode.ErrorCode;
import com.todongsan.insightreputation.global.response.ApiResponse;
import com.todongsan.insightreputation.insight.dto.MarketDashboardResponse;
import com.todongsan.insightreputation.insight.dto.MarketPriceHistoryResponse;
import com.todongsan.insightreputation.insight.dto.MarketPublicDataReferenceResponse;
import com.todongsan.insightreputation.insight.service.MarketPriceHistoryService;
import com.todongsan.insightreputation.insight.service.MarketPublicDataReferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Market Public Data Reference", description = "마켓 베팅 전 공공 데이터 참고 자료 조회")
public class MarketPublicDataReferenceController {

    private final MarketPublicDataReferenceService marketPublicDataReferenceService;
    private final MarketPriceHistoryService marketPriceHistoryService;

    @Operation(
        summary = "마켓 공공 데이터 참고 자료 조회",
        description = "마켓에서 베팅 전 참고할 수 있도록 공공 데이터 기반 시장 현황 요약을 제공한다. " +
                     "포인트 차감 없이 실시간으로 생성되며, 결과는 저장되지 않는다. " +
                     "마켓의 진행 상태(ACTIVE/SETTLED)와 무관하게 조회 가능하다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "참고 자료 조회 성공 (공공 데이터 없을 경우 안내 메시지 반환)"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "UNAUTHORIZED"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "503",
            description = "EXTERNAL_SERVICE_UNAVAILABLE (Market Service 연결 불가)"
        )
    })
    @GetMapping("/api/v1/insights/markets/{marketId}/public-data-reference")
    public ResponseEntity<ApiResponse<MarketPublicDataReferenceResponse>> getPublicDataReference(
            @PathVariable Long marketId,
            @Parameter(hidden = true)
            @RequestHeader("X-Member-Id") Long memberId) {

        log.info("마켓 공공 데이터 참고 자료 조회: marketId={}, memberId={}", marketId, memberId);

        MarketPublicDataReferenceResponse response =
                marketPublicDataReferenceService.getReference(marketId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
        summary = "마켓 가격 이력 조회",
        description = "관리자용. 마켓 지역의 공공 매매가격지수 시계열과 최종 예측 분포를 반환한다. " +
                     "주간 데이터(최근 8주) 우선, 없으면 월간(최근 6개월) 폴백. " +
                     "SETTLED 마켓이면 latestPredictionDistribution 포함, 그 외엔 빈 배열."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "조회 성공 (공공 데이터 없을 경우 priceHistory 빈 배열)"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "RESOURCE_NOT_FOUND (존재하지 않는 marketId)"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "503",
            description = "EXTERNAL_SERVICE_UNAVAILABLE (Market Service 연결 불가)"
        )
    })
    @GetMapping("/api/v1/admin/insights/markets/{marketId}/price-history")
    public ResponseEntity<ApiResponse<MarketPriceHistoryResponse>> getPriceHistory(
            @PathVariable Long marketId,
            @Parameter(hidden = true)
            @RequestHeader("X-Member-Id") Long memberId) {

        log.info("마켓 가격 이력 조회: marketId={}, memberId={}", marketId, memberId);

        return ResponseEntity.ok(ApiResponse.success(marketPriceHistoryService.getPriceHistory(marketId)));
    }

    @Operation(
        summary = "마켓 통합 대시보드 조회",
        description = "관리자용. 마켓의 가격 이력, 예측 분포, 참여자 인구통계, 방문 인증 현황을 통합 제공한다. " +
                     "SETTLED 마켓이면 predictionDistribution·priceVsPredictionOverlay·participantStats 포함."
    )
    @GetMapping("/api/v1/admin/insights/markets/{marketId}/dashboard")
    public ResponseEntity<ApiResponse<MarketDashboardResponse>> getDashboard(
            @PathVariable Long marketId,
            @RequestHeader(value = "X-Member-Role", required = false) String role) {
        if (!"ADMIN".equals(role)) throw new CustomException(ErrorCode.FORBIDDEN);
        log.info("마켓 통합 대시보드 조회: marketId={}", marketId);
        return ResponseEntity.ok(ApiResponse.success(marketPriceHistoryService.getDashboard(marketId)));
    }
}
