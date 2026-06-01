package com.todongsan.insightreputation.insight.scheduler;

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

@Slf4j
@Component
@RequiredArgsConstructor
public class InsightReportRecoveryScheduler {

    private final InsightReportRepository insightReportRepository;
    
    private static final int PROCESSING_TIMEOUT_MINUTES = 10;

    /**
     * PROCESSING 상태에서 고착된 리포트를 PENDING으로 리셋
     * 매 5분마다 실행
     */
    @Scheduled(fixedRate = 5 * 60 * 1000) // 5분마다 실행
    @Transactional
    public void recoverTimeoutReports() {
        log.debug("리포트 회복 스케줄러 시작");
        
        LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(PROCESSING_TIMEOUT_MINUTES);
        
        List<InsightReport> timeoutReports = insightReportRepository
                .findTimeoutProcessingReports(InsightReportStatus.PROCESSING, timeoutThreshold);
        
        if (timeoutReports.isEmpty()) {
            log.debug("회복 대상 리포트 없음");
            return;
        }
        
        log.warn("PROCESSING 타임아웃 리포트 발견: count={}", timeoutReports.size());
        
        for (InsightReport report : timeoutReports) {
            try {
                // 최대 재시도 횟수 초과 여부 확인
                if (report.getRetryCount() >= InsightReport.MAX_RETRY_COUNT) {
                    // 영구 실패로 전환
                    report.failPermanently("처리 타임아웃으로 인한 영구 실패");
                    log.error("리포트 영구 실패 처리: reportId={}, retryCount={}", 
                             report.getId(), report.getRetryCount());
                } else {
                    // PENDING으로 리셋하여 재시도 가능하도록 설정
                    report.resetForRetry();
                    log.warn("리포트 재시도 리셋: reportId={}, retryCount={}", 
                            report.getId(), report.getRetryCount());
                }
                
                insightReportRepository.save(report);
                
            } catch (Exception e) {
                log.error("리포트 회복 처리 중 오류: reportId={}", report.getId(), e);
            }
        }
        
        log.info("리포트 회복 스케줄러 완료: 처리된 리포트 수={}", timeoutReports.size());
    }
}