package com.todongsan.marketservice.market.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest
class SchedulerDisabledInTestContextTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void schedulerBeansAreDisabledInTestContext() {
        assertThat(applicationContext.getBeansOfType(PredictionSpendReconciliationScheduler.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(MarketSettlementRetryScheduler.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(MarketRefundRetryScheduler.class)).isEmpty();
    }
}
