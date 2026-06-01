package com.todongsan.insightreputation.visitcertification.controller.docs;

import com.todongsan.insightreputation.global.response.ApiResponse;
import com.todongsan.insightreputation.visitcertification.dto.VisitCertificationListResponse;
import com.todongsan.insightreputation.visitcertification.dto.VisitCertificationRequest;
import com.todongsan.insightreputation.visitcertification.dto.VisitCertificationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@Tag(name = "방문 인증", description = "GPS 또는 댓글 기반 지역 방문 인증 관련 API")
public interface VisitCertificationControllerDocs {

    @Operation(
        summary = "방문 인증 등록",
        description = """
            GPS 기반 방문 인증을 등록합니다.
            
            **비즈니스 규칙:**
            - 동일 지역 재인증 시 기존 레코드를 업데이트합니다 (INSERT 아님)
            - 쿨다운: 최근 인증으로부터 30일 경과 후 재인증 가능
            - GPS 반경: 지역 중심 좌표 기준 3km 이내만 허용
            - method가 GPS인 경우 data.latitude, data.longitude 필수
            """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "인증 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = VisitCertificationResponse.class),
                examples = @ExampleObject(
                    name = "성공 응답",
                    value = """
                        {
                          "success": true,
                          "data": {
                            "sido": "서울",
                            "sigu": "성동구",
                            "method": "GPS",
                            "certifiedAt": "2024-11-15T10:30:00",
                            "lastCertifiedAt": "2024-11-15T10:30:00",
                            "nextAvailableDate": "2024-12-15T10:30:00"
                          }
                        }
                        """
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "잘못된 요청",
            content = @Content(
                mediaType = "application/json",
                examples = {
                    @ExampleObject(
                        name = "반경 초과",
                        value = """
                            {
                              "success": false,
                              "errorCode": "VISIT_CERT_OUT_OF_RANGE",
                              "message": "GPS 인증 반경을 벗어났습니다"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "쿨다운 미경과",
                        value = """
                            {
                              "success": false,
                              "errorCode": "VISIT_CERT_COOLDOWN",
                              "message": "방문 인증 쿨다운 기간입니다"
                            }
                            """
                    )
                }
            )
        )
    })
    ApiResponse<VisitCertificationResponse> registerVisitCertification(
        @Parameter(hidden = true) @RequestHeader("X-Member-Id") Long memberId,
        
        @Parameter(
            description = "방문 인증 등록 요청",
            required = true,
            content = @Content(
                examples = @ExampleObject(
                    name = "GPS 인증 요청",
                    value = """
                        {
                          "sido": "서울",
                          "sigu": "성동구",
                          "method": "GPS",
                          "data": {
                            "latitude": 37.544876,
                            "longitude": 127.055678
                          }
                        }
                        """
                )
            )
        )
        @Valid @RequestBody VisitCertificationRequest request
    );

    @Operation(
        summary = "내 방문 인증 목록 조회",
        description = """
            현재 사용자의 모든 방문 인증 목록을 조회합니다.
            
            **응답 순서:** 인증일시 내림차순 (최신순)
            """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = List.class),
                examples = @ExampleObject(
                    name = "성공 응답",
                    value = """
                        {
                          "success": true,
                          "data": [
                            {
                              "sido": "서울",
                              "sigu": "성동구",
                              "method": "GPS",
                              "certifiedAt": "2024-11-15T10:30:00",
                              "lastCertifiedAt": "2024-11-15T10:30:00"
                            },
                            {
                              "sido": "서울",
                              "sigu": "강남구",
                              "method": "GPS",
                              "certifiedAt": "2024-11-10T14:20:00",
                              "lastCertifiedAt": "2024-11-10T14:20:00"
                            }
                          ]
                        }
                        """
                )
            )
        )
    })
    ApiResponse<List<VisitCertificationListResponse>> getMyVisitCertifications(
        @Parameter(hidden = true) @RequestHeader("X-Member-Id") Long memberId
    );
}