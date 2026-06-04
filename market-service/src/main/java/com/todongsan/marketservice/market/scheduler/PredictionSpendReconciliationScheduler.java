package com.todongsan.marketservice.market.scheduler;

import com.todongsan.marketservice.market.dto.response.ReconcilePredictionSpendResponse;
import com.todongsan.marketservice.market.service.PredictionSpendReconciliationService;
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
        prefix = "market.scheduler.prediction-reconciliation",
        name = "enabled",
        havingValue = "true"
)
public class PredictionSpendReconciliationScheduler {

    private final PredictionSpendReconciliationService predictionSpendReconciliationService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Value("${market.scheduler.prediction-reconciliation.limit:100}")
    private int limit;

    @Scheduled(fixedDelayString = "${market.scheduler.prediction-reconciliation.fixed-delay-ms:60000}")
    public void run() {
        if (!running.compareAndSet(false, true)) {
            log.info("Prediction reconciliation scheduler is already running. skip.");
            return;
        }
        try {
            ReconcilePredictionSpendResponse response = predictionSpendReconciliationService.reconcile(limit);
            log.info("Prediction reconciliation scheduler completed. response={}", response);
        } catch (Exception e) {
            log.error("Prediction reconciliation scheduler failed.", e);
        } finally {
            running.set(false);
        }
    }
}
