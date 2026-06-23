package com.todongsan.insightreputation.insight.controller.docs;

import com.todongsan.insightreputation.global.response.ApiResponse;
import com.todongsan.insightreputation.insight.dto.InsightReportResponse;
import com.todongsan.insightreputation.insight.dto.InsightReportStatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Insight Report", description = "AI 분석 리포트 생성 및 조회")
public interface InsightReportControllerDocs {

    @Operation(
        summary = "Battle AI 분석 자동 트리거 (내부 API)", 
        description = "Battle Service에서 Battle 종료 시 호출하는 내부 API. " +
                     "Point 차감 없이 자동으로 AI 분석 리포트를 생성한다. " +
                     "중복 트리거는 무시되며, 비동기로 분석을 수행한다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "자동 트리거 성공 (중복 트리거는 무시하고 200 반환)"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", 
            description = "INSIGHT_REPORT_SOURCE_NOT_CLOSED (Battle이 아직 종료되지 않음)"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404", 
            description = "RESOURCE_NOT_FOUND (존재하지 않는 battleId)"
        )
    })
    ResponseEntity<ApiResponse<Void>> triggerBattleReport(
        @Parameter(description = "Battle ID", example = "123") Long battleId
    );

    @Operation(
        summary = "Battle AI 분석 리포트 관리자 조회", 
        description = "관리자가 Battle AI 분석 리포트를 조회한다. " +
                     "모든 상태(PENDING, PROCESSING, DONE, FAILED)의 리포트를 조회 가능하며, " +
                     "재시도 횟수, 실패 사유 등 상세 정보를 포함한다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "관리자 리포트 조회 성공"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403", 
            description = "FORBIDDEN (ADMIN 권한 없음)"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404", 
            description = "RESOURCE_NOT_FOUND (Battle 없거나 리포트 없음)"
        )
    })
    ResponseEntity<ApiResponse<InsightReportResponse>> getAdminBattleReport(
        @Parameter(description = "Battle ID", example = "123") Long battleId,
        @Parameter(description = "회원 권한 (ADMIN 필수)", example = "ADMIN") String memberRole
    );

    @Operation(
        summary = "Battle AI 분석 리포트 생성 요청", 
        description = "종료된 Battle에 대해 AI 분석 리포트를 생성한다. " +
                     "80포인트가 차감되며, 기존 완료 리포트가 있는 경우 즉시 반환한다. " +
                     "리포트는 비동기로 생성되며, 상태 조회 API로 진행 상황을 확인할 수 있다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "리포트 생성 요청 성공 또는 기존 완료 리포트 반환"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", 
            description = "INSIGHT_REPORT_SOURCE_NOT_CLOSED (Battle이 아직 종료되지 않음) / POINT_INSUFFICIENT (포인트 부족)"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401", 
            description = "UNAUTHORIZED"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404", 
            description = "RESOURCE_NOT_FOUND (Battle을 찾을 수 없음)"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409", 
            description = "INSIGHT_REPORT_ALREADY_PROCESSING (이미 처리 중인 리포트 존재)"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "502", 
            description = "EXTERNAL_SERVICE_ERROR / EXTERNAL_SERVICE_UNAVAILABLE"
        )
    })
    ResponseEntity<ApiResponse<InsightReportResponse>> requestBattleReport(
        @Parameter(description = "Battle ID", example = "123") Long battleId,
        @Parameter(hidden = true) Long memberId,
        @Parameter(description = "멱등성 키 (중복 요청 방지)", example = "battle-report-123-20241201-user456") String idempotencyKey
    );
    
    @Operation(
        summary = "Battle AI 분석 리포트 상태 조회",
        description = "Battle AI 분석 리포트의 현재 상태를 조회한다. " +
                     "PENDING(대기중), PROCESSING(처리중), DONE(완료), FAILED(실패) 상태를 반환한다. " +
                     "클라이언트는 2초 간격으로 폴링하여 상태 변화를 확인할 수 있다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "리포트 상태 조회 성공"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404", 
            description = "INSIGHT_REPORT_NOT_FOUND (해당 Battle의 리포트가 존재하지 않음)"
        )
    })
    ResponseEntity<ApiResponse<InsightReportStatusResponse>> getBattleReportStatus(
        @Parameter(description = "Battle ID", example = "123") Long battleId
    );

    @Operation(
        summary = "Battle AI 분석 리포트 조회",
        description = "완료된 Battle AI 분석 리포트를 조회한다. " +
                     "DONE 상태의 리포트만 조회 가능하며, 포인트 차감 없이 결과를 반환한다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "리포트 조회 성공"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404", 
            description = "INSIGHT_REPORT_NOT_FOUND (완료된 리포트가 존재하지 않음)"
        )
    })
    ResponseEntity<ApiResponse<InsightReportResponse>> getBattleReport(
        @Parameter(description = "Battle ID", example = "123") Long battleId
    );

    @Operation(
        summary = "Market AI 분석 자동 트리거 (내부 API)",
        description = "Market Service에서 Market SETTLED 시 호출하는 내부 API. " +
                     "Point 차감 없이 자동으로 AI 분석 리포트를 생성한다. " +
                     "중복 트리거는 무시되며, 비동기로 분석을 수행한다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "자동 트리거 성공 (중복 트리거는 무시하고 200 반환)"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "INSIGHT_REPORT_SOURCE_DATA_NOT_READY (Market이 아직 SETTLED 상태가 아님)"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "RESOURCE_NOT_FOUND (존재하지 않는 marketId)"
        )
    })
    ResponseEntity<ApiResponse<Void>> triggerMarketReport(
        @Parameter(description = "Market ID", example = "456") Long marketId
    );

    @Operation(
        summary = "Market AI 정보 요약 생성 요청",
        description = "정산 완료된 Market에 대해 AI 정보 요약을 생성한다. " +
                     "80포인트가 차감되며, 기존 완료 리포트가 있는 경우 즉시 반환한다. " +
                     "리포트는 비동기로 생성되며, 상태 조회 API로 진행 상황을 확인할 수 있다. " +
                     "특정 선택지(YES/NO)를 추천하지 않고 균형 있는 정보 요약을 제공한다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "리포트 생성 요청 성공 또는 기존 완료 리포트 반환"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", 
            description = "POINT_INSUFFICIENT (포인트 부족)"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401", 
            description = "UNAUTHORIZED"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404", 
            description = "RESOURCE_NOT_FOUND (Market을 찾을 수 없음)"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409", 
            description = "INSIGHT_REPORT_ALREADY_PROCESSING (이미 처리 중인 리포트 존재) / INSIGHT_REPORT_SOURCE_DATA_NOT_READY (Market이 아직 정산되지 않음, 포인트 환불 처리됨)"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "502", 
            description = "EXTERNAL_SERVICE_ERROR / EXTERNAL_SERVICE_UNAVAILABLE"
        )
    })
    ResponseEntity<ApiResponse<InsightReportResponse>> requestMarketReport(
        @Parameter(description = "Market ID", example = "456") Long marketId,
        @Parameter(hidden = true) Long memberId,
        @Parameter(description = "멱등성 키 (중복 요청 방지)", example = "market-report-456-20241201-user789") String idempotencyKey
    );
    
    @Operation(
        summary = "Market AI 정보 요약 조회",
        description = "완료된 Market AI 정보 요약을 조회한다. " +
                     "DONE 상태의 리포트만 조회 가능하며, 포인트 차감 없이 결과를 반환한다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "리포트 조회 성공"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404", 
            description = "INSIGHT_REPORT_NOT_FOUND (완료된 리포트가 존재하지 않음)"
        )
    })
    ResponseEntity<ApiResponse<InsightReportResponse>> getMarketReport(
        @Parameter(description = "Market ID", example = "456") Long marketId
    );
    
    @Operation(
        summary = "Market AI 정보 요약 상태 조회",
        description = "Market AI 정보 요약의 현재 상태를 조회한다. " +
                     "PENDING(대기중), PROCESSING(처리중), DONE(완료), FAILED(실패) 상태를 반환한다. " +
                     "클라이언트는 2초 간격으로 폴링하여 상태 변화를 확인할 수 있다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "리포트 상태 조회 성공"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404", 
            description = "INSIGHT_REPORT_NOT_FOUND (해당 Market의 리포트가 존재하지 않음)"
        )
    })
    ResponseEntity<ApiResponse<InsightReportStatusResponse>> getMarketReportStatus(
        @Parameter(description = "Market ID", example = "456") Long marketId
    );
}