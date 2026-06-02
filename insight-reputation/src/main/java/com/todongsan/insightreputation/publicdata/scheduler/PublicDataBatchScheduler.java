package com.todongsan.insightreputation.publicdata.scheduler;

import com.todongsan.insightreputation.publicdata.service.PublicDataBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "scheduler.public-data.enabled", havingValue = "true", matchIfMissing = true)
public class PublicDataBatchScheduler {
    
    private final PublicDataBatchService batchService;
    
    /**
     * REB 주간 매매가격지수 배치 수집
     * 매주 목요일 오전 10시 실행
     */
    @Scheduled(cron = "${scheduler.reb.weekly.cron:0 0 10 * * THU}", zone = "Asia/Seoul")
    public void scheduleWeeklyPriceIndexCollection() {
        log.info("=== REB 주간 매매가격지수 배치 스케줄러 시작 ===");
        
        try {
            batchService.collectWeeklyPriceIndex();
            log.info("=== REB 주간 매매가격지수 배치 스케줄러 정상 완료 ===");
        } catch (Exception e) {
            log.error("=== REB 주간 매매가격지수 배치 스케줄러 실패 ===", e);
            // 예외를 다시 던지지 않음 - 스케줄러가 중단되지 않도록
        }
    }
    
    /**
     * REB 월간 매매가격지수 배치 수집
     * 매월 15일 오전 10시 실행
     */
    @Scheduled(cron = "${scheduler.reb.monthly.cron:0 0 10 15 * ?}", zone = "Asia/Seoul")
    public void scheduleMonthlyPriceIndexCollection() {
        log.info("=== REB 월간 매매가격지수 배치 스케줄러 시작 ===");
        
        try {
            batchService.collectMonthlyPriceIndex();
            log.info("=== REB 월간 매매가격지수 배치 스케줄러 정상 완료 ===");
        } catch (Exception e) {
            log.error("=== REB 월간 매매가격지수 배치 스케줄러 실패 ===", e);
            // 예외를 다시 던지지 않음 - 스케줄러가 중단되지 않도록
        }
    }
}