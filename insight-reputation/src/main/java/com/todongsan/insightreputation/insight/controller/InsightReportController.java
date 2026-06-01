package com.todongsan.insightreputation.insight.controller;

import com.todongsan.insightreputation.global.response.ApiResponse;
import com.todongsan.insightreputation.insight.controller.docs.InsightReportControllerDocs;
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
public class InsightReportController implements InsightReportControllerDocs {

    private final InsightReportService insightReportService;

    @PostMapping("/battles/{battleId}/report")
    @Override
    public ResponseEntity<ApiResponse<InsightReportResponse>> requestBattleReport(
            @PathVariable Long battleId,
            @RequestHeader("X-Member-Id") Long memberId) {
        
        log.info("Battle 리포트 생성 요청: battleId={}, memberId={}", battleId, memberId);
        
        InsightReportResponse response = insightReportService.requestBattleReport(memberId, battleId);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/battles/{battleId}/report/status")
    @Override
    public ResponseEntity<ApiResponse<InsightReportStatusResponse>> getBattleReportStatus(
            @PathVariable Long battleId) {
        
        log.info("Battle 리포트 상태 조회: battleId={}", battleId);
        
        InsightReportStatusResponse response = insightReportService.getBattleReportStatus(battleId);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}