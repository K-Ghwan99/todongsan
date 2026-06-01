package com.todongsan.insightreputation.reputation.controller.docs;

import com.todongsan.insightreputation.global.response.ApiResponse;
import com.todongsan.insightreputation.reputation.dto.request.ChangeResidenceRequest;
import com.todongsan.insightreputation.reputation.dto.response.MyReputationResponse;
import com.todongsan.insightreputation.reputation.dto.response.ReputationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Reputation", description = "신뢰도 조회 및 거주지역 관리")
public interface ReputationControllerDocs {

    @Operation(summary = "내 신뢰도 조회", description = "로그인한 사용자의 신뢰도 정보와 방문 인증 목록을 반환한다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "UNAUTHORIZED"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "REPUTATION_NOT_FOUND")
    })
    ApiResponse<MyReputationResponse> getMyReputation(
        @Parameter(hidden = true) Long memberId
    );

    @Operation(summary = "특정 회원 신뢰도 조회", description = "특정 회원의 신뢰도 정보를 반환한다. 민감한 정보(방문 인증 상세)는 제외된다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "REPUTATION_NOT_FOUND")
    })
    ApiResponse<ReputationResponse> getReputation(
        @Parameter(description = "조회할 회원 ID") Long memberId
    );

    @Operation(summary = "거주지역 선언/변경", description = "거주지역을 최초 선언하거나 변경한다. 변경 시 30일 쿨다운이 적용된다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "선언/변경 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "REPUTATION_RESIDENCE_CHANGE_COOLDOWN"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
    })
    ApiResponse<Void> changeResidence(
        @Parameter(hidden = true) Long memberId,
        ChangeResidenceRequest request
    );
}