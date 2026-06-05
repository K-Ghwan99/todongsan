package com.todongsan.marketservice.market.scheduler;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.todongsan.marketservice.market.dto.response.ReconcilePredictionSpendResponse;
import com.todongsan.marketservice.market.service.PredictionSpendReconciliationService;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PredictionSpendReconciliationSchedulerTest {

    @Mock
    private PredictionSpendReconciliationService predictionSpendReconciliationService;

    @Test
    void runCallsReconciliationService() {
        PredictionSpendReconciliationScheduler scheduler = scheduler(123);
        when(predictionSpendReconciliationService.reconcile(123))
                .thenReturn(new ReconcilePredictionSpendResponse(123, 0, 0, 0, 0, 0, 0, 0));

        scheduler.run();

        verify(predictionSpendReconciliationService).reconcile(123);
    }

    @Test
    void runSwallowsExceptionAndRestoresRunningFlag() {
        PredictionSpendReconciliationScheduler scheduler = scheduler(100);
        when(predictionSpendReconciliationService.reconcile(100))
                .thenThrow(new IllegalStateException("boom"))
                .thenReturn(new ReconcilePredictionSpendResponse(100, 0, 0, 0, 0, 0, 0, 0));

        scheduler.run();
        scheduler.run();

        verify(predictionSpendReconciliationService, times(2)).reconcile(100);
    }

    @Test
    void runSkipsWhenAlreadyRunning() {
        PredictionSpendReconciliationScheduler scheduler = scheduler(100);
        ReflectionTestUtils.setField(scheduler, "running", new AtomicBoolean(true));

        scheduler.run();

        verify(predictionSpendReconciliationService, never()).reconcile(100);
    }

    private PredictionSpendReconciliationScheduler scheduler(int limit) {
        PredictionSpendReconciliationScheduler scheduler =
                new PredictionSpendReconciliationScheduler(predictionSpendReconciliationService);
        ReflectionTestUtils.setField(scheduler, "limit", limit);
        return scheduler;
    }
}
