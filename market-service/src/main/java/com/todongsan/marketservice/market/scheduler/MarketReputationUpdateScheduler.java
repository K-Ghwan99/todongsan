package com.todongsan.marketservice.market.scheduler;

import com.todongsan.marketservice.market.service.MarketReputationUpdateService;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "market.scheduler.reputation-update",
        name = "enabled",
        havingValue = "true"
)
public class MarketReputationUpdateScheduler {

    private final MarketReputationUpdateService marketReputationUpdateService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Value("${market.scheduler.reputation-update.limit:50}")
    private int limit;

    @Scheduled(fixedDelayString = "${market.scheduler.reputation-update.fixed-delay-ms:180000}")
    public void run() {
        if (!running.compareAndSet(false, true)) {
            log.info("Reputation update scheduler is already running. skip.");
            return;
        }
        try {
            int processedCount = marketReputationUpdateService.processPendingOrUnknownUpdates(limit);
            log.info("Reputation update scheduler completed. processedCount={}", processedCount);
        } catch (Exception e) {
            log.error("Reputation update scheduler failed.", e);
        } finally {
            running.set(false);
        }
    }
}
