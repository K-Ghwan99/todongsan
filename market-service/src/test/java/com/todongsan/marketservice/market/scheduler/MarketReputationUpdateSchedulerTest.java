package com.todongsan.marketservice.market.scheduler;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.todongsan.marketservice.market.service.MarketReputationUpdateService;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MarketReputationUpdateSchedulerTest {

    @Mock
    private MarketReputationUpdateService marketReputationUpdateService;

    @Test
    void runCallsReputationUpdateService() {
        MarketReputationUpdateScheduler scheduler = scheduler(30);
        when(marketReputationUpdateService.processPendingOrUnknownUpdates(30)).thenReturn(2);

        scheduler.run();

        verify(marketReputationUpdateService).processPendingOrUnknownUpdates(30);
    }

    @Test
    void runSwallowsExceptionAndRestoresRunningFlag() {
        MarketReputationUpdateScheduler scheduler = scheduler(50);
        when(marketReputationUpdateService.processPendingOrUnknownUpdates(50))
                .thenThrow(new IllegalStateException("boom"))
                .thenReturn(0);

        scheduler.run();
        scheduler.run();

        verify(marketReputationUpdateService, times(2)).processPendingOrUnknownUpdates(50);
    }

    @Test
    void runSkipsWhenAlreadyRunning() {
        MarketReputationUpdateScheduler scheduler = scheduler(50);
        ReflectionTestUtils.setField(scheduler, "running", new AtomicBoolean(true));

        scheduler.run();

        verify(marketReputationUpdateService, never()).processPendingOrUnknownUpdates(50);
    }

    private MarketReputationUpdateScheduler scheduler(int limit) {
        MarketReputationUpdateScheduler scheduler = new MarketReputationUpdateScheduler(marketReputationUpdateService);
        ReflectionTestUtils.setField(scheduler, "limit", limit);
        return scheduler;
    }
}
