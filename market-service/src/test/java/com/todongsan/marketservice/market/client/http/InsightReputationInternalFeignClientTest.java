package com.todongsan.marketservice.market.client.http;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

class InsightReputationInternalFeignClientTest {

    @Test
    void feignClientUsesConfiguredBaseUrl() {
        FeignClient annotation = InsightReputationInternalFeignClient.class.getAnnotation(FeignClient.class);

        assertThat(annotation.name()).isEqualTo("insight-reputation-internal");
        assertThat(annotation.url()).isEqualTo("${client.insight.base-url:http://localhost:8083}");
    }

    @Test
    void feignClientUsesInternalPredictionAccuracyEndpoint() throws NoSuchMethodException {
        assertThat(postMapping(
                "updatePredictionAccuracy",
                InsightReputationHttpDtos.PredictionAccuracyUpdateRequest.class
        )).containsExactly("/internal/api/v1/reputations/prediction");
    }

    @Test
    void updatePredictionAccuracyUsesOnlyRequestBodyWithoutHeaders() throws NoSuchMethodException {
        Method method = InsightReputationInternalFeignClient.class.getMethod(
                "updatePredictionAccuracy",
                InsightReputationHttpDtos.PredictionAccuracyUpdateRequest.class
        );

        assertThat(hasRequestBody(method.getParameterAnnotations()[0])).isTrue();
        assertThat(hasRequestHeader(method.getParameterAnnotations()[0])).isFalse();
    }

    @Test
    void requestBodyContainsInsightContractFields() {
        InsightReputationHttpDtos.PredictionAccuracyUpdateRequest request =
                new InsightReputationHttpDtos.PredictionAccuracyUpdateRequest(1L, 7L, 123L, true);

        assertThat(request.memberId()).isEqualTo(1L);
        assertThat(request.marketId()).isEqualTo(7L);
        assertThat(request.predictionId()).isEqualTo(123L);
        assertThat(request.isCorrect()).isTrue();
    }

    private String[] postMapping(String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        return InsightReputationInternalFeignClient.class.getMethod(methodName, parameterTypes)
                .getAnnotation(PostMapping.class)
                .value();
    }

    private boolean hasRequestBody(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation instanceof RequestBody) {
                return true;
            }
        }
        return false;
    }

    private boolean hasRequestHeader(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation instanceof RequestHeader) {
                return true;
            }
        }
        return false;
    }
}
