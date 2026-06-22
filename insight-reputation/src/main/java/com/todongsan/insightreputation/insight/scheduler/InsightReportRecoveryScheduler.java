package com.todongsan.insightreputation.insight.scheduler;

import com.todongsan.insightreputation.enums.InsightReportStatus;
import com.todongsan.insightreputation.enums.InsightReportType;
import com.todongsan.insightreputation.insight.entity.InsightReport;
import com.todongsan.insightreputation.insight.repository.InsightReportRepository;
import com.todongsan.insightreputation.insight.service.InsightReportService;
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
    private final InsightReportService insightReportService;

    private static final int PROCESSING_TIMEOUT_MINUTES = 10;
    private static final int PENDING_ORPHAN_MINUTES = 3;

    /**
     * PROCESSING 상태에서 고착된 리포트를 PENDING으로 리셋
     * 매 5분마다 실행
     */
    @Scheduled(fixedRate = 5 * 60 * 1000)
    @Transactional
    public void recoverTimeoutReports() {
        LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(PROCESSING_TIMEOUT_MINUTES);

        List<InsightReport> timeoutReports = insightReportRepository
                .findTimeoutProcessingReports(InsightReportStatus.PROCESSING, timeoutThreshold);

        if (timeoutReports.isEmpty()) return;

        log.warn("PROCESSING 타임아웃 리포트 발견: count={}", timeoutReports.size());

        for (InsightReport report : timeoutReports) {
            try {
                if (report.getRetryCount() >= InsightReport.MAX_RETRY_COUNT) {
                    report.fail("처리 타임아웃으로 인한 영구 실패");
                    log.error("리포트 영구 실패 처리: reportId={}, retryCount={}",
                            report.getId(), report.getRetryCount());
                } else {
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

    /**
     * FAILED 상태 리포트를 PENDING으로 리셋
     * 매 10분마다 실행
     */
    @Scheduled(fixedRate = 10 * 60 * 1000)
    @Transactional
    public void retryFailedReports() {
        List<InsightReport> failedReports = insightReportRepository
                .findByStatusAndRetryCountLessThan(InsightReportStatus.FAILED, (byte) InsightReport.MAX_RETRY_COUNT);

        if (failedReports.isEmpty()) return;

        log.info("재시도 가능한 실패 리포트 발견: count={}", failedReports.size());

        for (InsightReport report : failedReports) {
            try {
                report.resetForRetry();
                insightReportRepository.save(report);
                log.info("실패 리포트 재시도 리셋: reportId={}, retryCount={}",
                        report.getId(), report.getRetryCount());
            } catch (Exception e) {
                log.error("실패 리포트 재시도 처리 중 오류: reportId={}", report.getId(), e);
            }
        }

        log.info("실패 리포트 재시도 스케줄러 완료: 처리된 리포트 수={}", failedReports.size());
    }

    /**
     * 서버 재시작이나 비동기 실패로 인해 방치된 PENDING 리포트를 재처리
     * PENDING_ORPHAN_MINUTES 이상 PENDING 상태인 리포트에 비동기 분석 트리거
     * 매 5분마다 실행 (recoverTimeoutReports와 엇갈려 실행되도록 initialDelay 적용)
     */
    @Scheduled(fixedRate = 5 * 60 * 1000, initialDelay = 2 * 60 * 1000)
    public void processPendingReports() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(PENDING_ORPHAN_MINUTES);

        List<InsightReport> orphaned = insightReportRepository
                .findOrphanedPendingReports(InsightReportStatus.PENDING, cutoff);

        if (orphaned.isEmpty()) return;

        log.info("방치된 PENDING 리포트 발견, 재처리 트리거: count={}", orphaned.size());

        for (InsightReport report : orphaned) {
            try {
                if (report.getType() == InsightReportType.BATTLE) {
                    insightReportService.generateBattleReportAsync(report.getId());
                } else if (report.getType() == InsightReportType.MARKET) {
                    insightReportService.generateMarketReportAsync(report.getId());
                }
                log.info("PENDING 리포트 재처리 트리거 완료: reportId={}, type={}",
                        report.getId(), report.getType());
            } catch (Exception e) {
                log.error("PENDING 리포트 재처리 트리거 실패: reportId={}", report.getId(), e);
            }
        }
    }
}