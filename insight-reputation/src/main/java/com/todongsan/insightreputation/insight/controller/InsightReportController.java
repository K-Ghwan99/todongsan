package com.todongsan.insightreputation.insight.controller;

import com.todongsan.insightreputation.enums.InsightReportStatus;
import com.todongsan.insightreputation.enums.InsightReportType;
import com.todongsan.insightreputation.global.exception.CustomException;
import com.todongsan.insightreputation.global.exception.errorcode.ErrorCode;
import com.todongsan.insightreputation.global.response.ApiResponse;
import com.todongsan.insightreputation.insight.controller.docs.InsightReportControllerDocs;
import com.todongsan.insightreputation.insight.dto.InsightReportResponse;
import com.todongsan.insightreputation.insight.dto.InsightReportStatusResponse;
import com.todongsan.insightreputation.insight.entity.InsightReport;
import com.todongsan.insightreputation.insight.repository.InsightReportRepository;
import com.todongsan.insightreputation.insight.service.InsightReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/v1/insights")
@RequiredArgsConstructor
public class InsightReportController implements InsightReportControllerDocs {

    private final InsightReportService insightReportService;
    private final InsightReportRepository insightReportRepository;

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

    @PostMapping("/markets/{marketId}/report")
    @Override
    public ResponseEntity<ApiResponse<InsightReportResponse>> requestMarketReport(
            @PathVariable Long marketId,
            @RequestHeader("X-Member-Id") Long memberId) {
        
        log.info("Market 리포트 생성 요청: marketId={}, memberId={}", marketId, memberId);
        
        InsightReportResponse response = insightReportService.requestMarketReport(memberId, marketId);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/markets/{marketId}/report")
    @Override
    public ResponseEntity<ApiResponse<InsightReportResponse>> getMarketReport(
            @PathVariable Long marketId) {
        
        log.info("Market 리포트 조회: marketId={}", marketId);
        
        // Market 리포트 조회는 완료된 리포트만 반환
        Optional<InsightReport> reportOpt = insightReportRepository
                .findByTypeAndReferenceId(InsightReportType.MARKET, marketId);
        
        if (reportOpt.isEmpty()) {
            throw new CustomException(ErrorCode.INSIGHT_REPORT_NOT_FOUND);
        }
        
        InsightReport report = reportOpt.get();
        
        if (report.getStatus() != InsightReportStatus.DONE) {
            throw new CustomException(ErrorCode.INSIGHT_REPORT_NOT_FOUND);
        }
        
        InsightReportResponse response = InsightReportResponse.builder()
                .reportId(report.getId())
                .status(report.getStatus().name())
                .reportContent(report.getReportContent())
                .generatedAt(report.getGeneratedAt())
                .pointCharged(0)  // 조회 시에는 차감 없음
                .build();
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/markets/{marketId}/report/status")
    @Override
    public ResponseEntity<ApiResponse<InsightReportStatusResponse>> getMarketReportStatus(
            @PathVariable Long marketId) {
        
        log.info("Market 리포트 상태 조회: marketId={}", marketId);
        
        InsightReportStatusResponse response = insightReportService.getMarketReportStatus(marketId);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}