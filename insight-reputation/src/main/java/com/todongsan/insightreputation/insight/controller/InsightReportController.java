package com.todongsan.insightreputation.insight.controller;

import com.todongsan.insightreputation.global.response.ApiResponse;
import com.todongsan.insightreputation.insight.dto.InsightReportResponse;
import com.todongsan.insightreputation.insight.dto.InsightReportStatusResponse;
import com.todongsan.insightreputation.insight.service.InsightReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/insights")
@RequiredArgsConstructor
public class InsightReportController {

    private final InsightReportService insightReportService;

    /**
     * Battle AI 분석 리포트 생성 요청
     * 
     * @param battleId Battle ID
     * @param memberId 요청 회원 ID (헤더에서 추출)
     * @return 리포트 응답
     */
    @PostMapping("/battles/{battleId}/report")
    public ResponseEntity<ApiResponse<InsightReportResponse>> requestBattleReport(
            @PathVariable Long battleId,
            @RequestHeader("Member-Id") Long memberId) {
        
        log.info("Battle 리포트 생성 요청: battleId={}, memberId={}", battleId, memberId);
        
        InsightReportResponse response = insightReportService.requestBattleReport(memberId, battleId);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * Battle AI 분석 리포트 상태 조회
     * 
     * @param battleId Battle ID
     * @return 리포트 상태 응답
     */
    @GetMapping("/battles/{battleId}/report/status")
    public ResponseEntity<ApiResponse<InsightReportStatusResponse>> getBattleReportStatus(
            @PathVariable Long battleId) {
        
        log.info("Battle 리포트 상태 조회: battleId={}", battleId);
        
        InsightReportStatusResponse response = insightReportService.getBattleReportStatus(battleId);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}