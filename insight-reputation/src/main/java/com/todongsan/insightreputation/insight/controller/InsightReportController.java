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
@RequiredArgsConstructor
public class InsightReportController implements InsightReportControllerDocs {

    private final InsightReportService insightReportService;

    // ========== 내부 API ==========
    
    /**
     * Battle 자동 트리거 (내부 API)
     * Battle Service에서 Battle 종료 시 호출
     */
    @PostMapping("/internal/api/v1/insights/battles/{battleId}/report")
    @Override  
    public ResponseEntity<ApiResponse<Void>> triggerBattleReport(@PathVariable Long battleId) {
        log.info("Battle 자동 트리거: battleId={}", battleId);
        
        insightReportService.triggerBattleReport(battleId);
        
        return ResponseEntity.ok(ApiResponse.success(null));
    }
    
    // ========== 관리자 API ==========
    
    /**
     * Battle 리포트 관리자 조회
     */
    @GetMapping("/api/v1/admin/insights/battles/{battleId}/report")
    @Override
    public ResponseEntity<ApiResponse<InsightReportResponse>> getAdminBattleReport(
            @PathVariable Long battleId,
            @RequestHeader(value = "X-Member-Role", required = true) String memberRole) {
        
        log.info("Battle 리포트 관리자 조회: battleId={}, memberRole={}", battleId, memberRole);
        
        // ADMIN 권한 확인
        if (!"ADMIN".equals(memberRole)) {
            log.warn("ADMIN 권한 없음: memberRole={}, battleId={}", memberRole, battleId);
            return ResponseEntity.status(403).build();
        }
        
        InsightReportResponse response = insightReportService.getAdminBattleReport(battleId);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ========== 사용자 API (미사용) ==========

    @PostMapping("/api/v1/insights/battles/{battleId}/report")
    @Override
    public ResponseEntity<ApiResponse<InsightReportResponse>> requestBattleReport(
            @PathVariable Long battleId,
            @RequestHeader("X-Member-Id") Long memberId,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        
        log.info("Battle 리포트 생성 요청: battleId={}, memberId={}, idempotencyKey={}", battleId, memberId, idempotencyKey);
        
        InsightReportResponse response = insightReportService.requestBattleReport(memberId, battleId, idempotencyKey);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/api/v1/insights/battles/{battleId}/report")
    @Override
    public ResponseEntity<ApiResponse<InsightReportResponse>> getBattleReport(
            @PathVariable Long battleId) {
        
        log.info("Battle 리포트 조회: battleId={}", battleId);
        
        InsightReportResponse response = insightReportService.getBattleReport(battleId);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/api/v1/insights/battles/{battleId}/report/status")
    @Override
    public ResponseEntity<ApiResponse<InsightReportStatusResponse>> getBattleReportStatus(
            @PathVariable Long battleId) {
        
        log.info("Battle 리포트 상태 조회: battleId={}", battleId);
        
        InsightReportStatusResponse response = insightReportService.getBattleReportStatus(battleId);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/api/v1/insights/markets/{marketId}/report")
    @Override
    public ResponseEntity<ApiResponse<InsightReportResponse>> requestMarketReport(
            @PathVariable Long marketId,
            @RequestHeader("X-Member-Id") Long memberId,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        
        log.info("Market 리포트 생성 요청: marketId={}, memberId={}, idempotencyKey={}", marketId, memberId, idempotencyKey);
        
        InsightReportResponse response = insightReportService.requestMarketReport(memberId, marketId, idempotencyKey);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/api/v1/insights/markets/{marketId}/report")
    @Override
    public ResponseEntity<ApiResponse<InsightReportResponse>> getMarketReport(
            @PathVariable Long marketId) {
        
        log.info("Market 리포트 조회: marketId={}", marketId);
        
        InsightReportResponse response = insightReportService.getMarketReport(marketId);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/api/v1/insights/markets/{marketId}/report/status")
    @Override
    public ResponseEntity<ApiResponse<InsightReportStatusResponse>> getMarketReportStatus(
            @PathVariable Long marketId) {
        
        log.info("Market 리포트 상태 조회: marketId={}", marketId);
        
        InsightReportStatusResponse response = insightReportService.getMarketReportStatus(marketId);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}