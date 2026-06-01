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
        @Parameter(hidden = true) Long memberId
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
}