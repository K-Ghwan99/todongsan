package com.todongsan.insightreputation.publicdata.controller;

import com.todongsan.insightreputation.global.response.ApiResponse;
import com.todongsan.insightreputation.publicdata.service.PublicDataBatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Public Data Batch", description = "공공 데이터 배치 수집 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/public-data")
@RequiredArgsConstructor
public class PublicDataBatchController {
    
    private final PublicDataBatchService batchService;
    
    @Operation(
        summary = "REB 주간 매매가격지수 수동 수집",
        description = "한국부동산원 R-ONE API에서 주간 매매가격지수 데이터를 수동으로 수집합니다. " +
                     "정상적으로는 매주 금요일 오전 10시에 자동 실행됩니다."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "배치 수집 성공"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "외부 API 호출 실패 또는 데이터 처리 오류"
        )
    })
    @PostMapping("/batch/weekly-price-index")
    public ResponseEntity<ApiResponse<String>> collectWeeklyPriceIndex() {
        log.info("REB 주간 매매가격지수 수동 배치 요청");
        
        try {
            batchService.collectWeeklyPriceIndex();
            return ResponseEntity.ok(ApiResponse.success("REB 주간 매매가격지수 배치 수집 완료"));
        } catch (Exception e) {
            log.error("REB 주간 매매가격지수 수동 배치 실패", e);
            throw e;
        }
    }
    
    @Operation(
        summary = "REB 월간 매매가격지수 수동 수집",
        description = "한국부동산원 R-ONE API에서 월간 매매가격지수 데이터를 수동으로 수집합니다. " +
                     "정상적으로는 매월 16일 오전 10시에 자동 실행됩니다."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "배치 수집 성공"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "외부 API 호출 실패 또는 데이터 처리 오류"
        )
    })
    @PostMapping("/batch/monthly-price-index")
    public ResponseEntity<ApiResponse<String>> collectMonthlyPriceIndex() {
        log.info("REB 월간 매매가격지수 수동 배치 요청");
        
        try {
            batchService.collectMonthlyPriceIndex();
            return ResponseEntity.ok(ApiResponse.success("REB 월간 매매가격지수 배치 수집 완료"));
        } catch (Exception e) {
            log.error("REB 월간 매매가격지수 수동 배치 실패", e);
            throw e;
        }
    }
}