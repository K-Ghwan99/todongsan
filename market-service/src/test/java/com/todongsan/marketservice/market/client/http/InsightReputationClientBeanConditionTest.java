package com.todongsan.marketservice.market.client.http;

import static org.assertj.core.api.Assertions.assertThat;

import com.todongsan.marketservice.market.client.FakeInsightReputationClient;
import com.todongsan.marketservice.market.client.InsightReputationClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

class InsightReputationClientBeanConditionTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Test
    void fakeClientIsDefaultInsightReputationClient() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(InsightReputationClient.class);
            assertThat(context).hasSingleBean(FakeInsightReputationClient.class);
            assertThat(context).doesNotHaveBean(HttpInsightReputationClient.class);
        });
    }

    @Test
    void fakeClientIsActiveWhenModeIsFake() {
        contextRunner
                .withPropertyValues("client.insight.mode=fake")
                .run(context -> {
                    assertThat(context).hasSingleBean(InsightReputationClient.class);
                    assertThat(context).hasSingleBean(FakeInsightReputationClient.class);
                    assertThat(context).doesNotHaveBean(HttpInsightReputationClient.class);
                });
    }

    @Test
    void httpClientIsActiveOnlyWhenModeIsHttp() {
        contextRunner
                .withPropertyValues("client.insight.mode=http")
                .run(context -> {
                    assertThat(context).hasSingleBean(InsightReputationClient.class);
                    assertThat(context).hasSingleBean(HttpInsightReputationClient.class);
                    assertThat(context).doesNotHaveBean(FakeInsightReputationClient.class);
                });
    }

    @Configuration
    @Import({FakeInsightReputationClient.class, HttpInsightReputationClient.class})
    static class TestConfig {

        @Bean
        InsightReputationInternalFeignClient insightReputationInternalFeignClient() {
            return request -> null;
        }
    }
}
