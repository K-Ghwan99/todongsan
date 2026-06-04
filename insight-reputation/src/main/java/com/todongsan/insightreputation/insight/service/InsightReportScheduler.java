package com.todongsan.insightreputation.insight.service;

import com.todongsan.insightreputation.enums.InsightReportStatus;
import com.todongsan.insightreputation.insight.entity.InsightReport;
import com.todongsan.insightreputation.insight.repository.InsightReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Insight Report 회복 스케줄러
 * - PROCESSING 10분 고착 상태 복구
 * - FAILED 재시도 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InsightReportScheduler {

    private final InsightReportRepository insightReportRepository;

    /**
     * PROCESSING 상태 고착 복구
     * processingStartedAt + 10분 경과한 리포트를 PENDING으로 리셋
     */
    @Scheduled(fixedDelay = 60000) // 1분마다 실행
    @Transactional
    public void resetStuckProcessing() {
        LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(10);
        
        List<InsightReport> stuckReports = insightReportRepository
                .findTimeoutProcessingReports(InsightReportStatus.PROCESSING, timeoutThreshold);
        
        if (stuckReports.isEmpty()) {
            return;
        }
        
        log.warn("PROCESSING 고착 상태 리포트 발견: count={}", stuckReports.size());
        
        for (InsightReport report : stuckReports) {
            try {
                log.info("PROCESSING 고착 리포트 PENDING 리셋: reportId={}, processingStartedAt={}", 
                        report.getId(), report.getProcessingStartedAt());
                
                report.resetForRetry();
                insightReportRepository.save(report);
                
                log.info("PROCESSING 고착 리포트 복구 완료: reportId={}", report.getId());
                
            } catch (Exception e) {
                log.error("PROCESSING 고착 리포트 복구 실패: reportId={}", report.getId(), e);
            }
        }
        
        log.info("PROCESSING 고착 상태 복구 완료: resetCount={}", stuckReports.size());
    }

    /**
     * FAILED 상태 재시도 처리
     * retry_count < 3인 FAILED 리포트를 PENDING으로 리셋
     */
    @Scheduled(fixedDelay = 60000) // 1분마다 실행
    @Transactional
    public void retryFailedReports() {
        List<InsightReport> retryableReports = insightReportRepository
                .findByStatusAndRetryCountLessThan(InsightReportStatus.FAILED, (byte) InsightReport.MAX_RETRY_COUNT);
        
        if (retryableReports.isEmpty()) {
            return;
        }
        
        log.info("재시도 가능한 FAILED 리포트 발견: count={}", retryableReports.size());
        
        for (InsightReport report : retryableReports) {
            try {
                log.info("FAILED 리포트 재시도: reportId={}, retryCount={}", 
                        report.getId(), report.getRetryCount());
                
                report.resetForRetry();
                insightReportRepository.save(report);
                
                log.info("FAILED 리포트 재시도 설정 완료: reportId={}, newRetryCount={}", 
                        report.getId(), report.getRetryCount());
                
            } catch (Exception e) {
                log.error("FAILED 리포트 재시도 설정 실패: reportId={}", report.getId(), e);
            }
        }
        
        log.info("FAILED 리포트 재시도 처리 완료: retryCount={}", retryableReports.size());
    }

    /**
     * 스케줄러 상태 로깅 (디버깅용)
     */
    @Scheduled(fixedDelay = 300000) // 5분마다 실행
    @Transactional(readOnly = true)
    public void logReportStatistics() {
        try {
            long pendingCount = insightReportRepository.findByStatus(InsightReportStatus.PENDING).size();
            long processingCount = insightReportRepository.findByStatus(InsightReportStatus.PROCESSING).size();
            long doneCount = insightReportRepository.findByStatus(InsightReportStatus.DONE).size();
            long failedCount = insightReportRepository.findByStatus(InsightReportStatus.FAILED).size();
            
            log.info("Insight Report 상태 통계 - PENDING: {}, PROCESSING: {}, DONE: {}, FAILED: {}", 
                    pendingCount, processingCount, doneCount, failedCount);
                    
        } catch (Exception e) {
            log.error("리포트 통계 로깅 중 오류", e);
        }
    }
}