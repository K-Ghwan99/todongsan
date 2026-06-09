package com.todongsan.marketservice.market.client.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.todongsan.marketservice.market.client.PredictionAccuracyUpdateCommand;
import com.todongsan.marketservice.market.client.exception.InsightReputationExternalException;
import com.todongsan.marketservice.market.client.exception.InsightReputationFailedException;
import com.todongsan.marketservice.market.client.exception.InsightReputationTimeoutException;
import com.todongsan.marketservice.market.client.exception.InsightReputationUnavailableException;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import feign.RetryableException;
import feign.codec.DecodeException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HttpInsightReputationClientTest {

    @Test
    void updatePredictionAccuracyReturnsUnwrappedResult() {
        CapturingFeignClient feignClient = new CapturingFeignClient();
        feignClient.response = ok(new InsightReputationHttpDtos.PredictionAccuracyUpdateResponse(
                1L,
                11,
                8,
                new BigDecimal("72.73")
        ));
        HttpInsightReputationClient client = new HttpInsightReputationClient(feignClient);

        var result = client.updatePredictionAccuracy(command());

        assertThat(feignClient.request.memberId()).isEqualTo(1L);
        assertThat(feignClient.request.marketId()).isEqualTo(7L);
        assertThat(feignClient.request.predictionId()).isEqualTo(123L);
        assertThat(feignClient.request.isCorrect()).isTrue();
        assertThat(result.memberId()).isEqualTo(1L);
        assertThat(result.predictionCount()).isEqualTo(11);
        assertThat(result.predictionCorrect()).isEqualTo(8);
        assertThat(result.predictionAccuracy()).isEqualByComparingTo("72.73");
    }

    @Test
    void updatePredictionAccuracyThrowsExternalExceptionWhenDataIsNull() {
        CapturingFeignClient feignClient = new CapturingFeignClient();
        feignClient.response = ok(null);
        HttpInsightReputationClient client = new HttpInsightReputationClient(feignClient);

        assertThatThrownBy(() -> client.updatePredictionAccuracy(command()))
                .isInstanceOf(InsightReputationExternalException.class)
                .hasMessage("INSIGHT_REPUTATION_RESULT_UNKNOWN");
    }

    @Test
    void updatePredictionAccuracyMapsResourceNotFoundToFailedException() {
        CapturingFeignClient feignClient = new CapturingFeignClient();
        feignClient.response = fail("RESOURCE_NOT_FOUND", "not found");
        HttpInsightReputationClient client = new HttpInsightReputationClient(feignClient);

        assertThatThrownBy(() -> client.updatePredictionAccuracy(command()))
                .isInstanceOf(InsightReputationFailedException.class)
                .hasMessage("RESOURCE_NOT_FOUND");
    }

    @Test
    void updatePredictionAccuracyMapsValidationFailedToFailedException() {
        CapturingFeignClient feignClient = new CapturingFeignClient();
        feignClient.response = fail("VALIDATION_FAILED", "invalid");
        HttpInsightReputationClient client = new HttpInsightReputationClient(feignClient);

        assertThatThrownBy(() -> client.updatePredictionAccuracy(command()))
                .isInstanceOf(InsightReputationFailedException.class)
                .hasMessage("VALIDATION_FAILED");
    }

    @Test
    void updatePredictionAccuracyMapsRetryableExceptionToTimeoutException() {
        CapturingFeignClient feignClient = new CapturingFeignClient();
        feignClient.exception = new RetryableException(
                504,
                "timeout",
                Request.HttpMethod.POST,
                null,
                new Date(),
                request()
        );
        HttpInsightReputationClient client = new HttpInsightReputationClient(feignClient);

        assertThatThrownBy(() -> client.updatePredictionAccuracy(command()))
                .isInstanceOf(InsightReputationTimeoutException.class);
    }

    @Test
    void updatePredictionAccuracyMapsFeign5xxToUnavailableException() {
        CapturingFeignClient feignClient = new CapturingFeignClient();
        feignClient.exception = FeignException.errorStatus(
                "updatePredictionAccuracy",
                feign.Response.builder()
                        .status(503)
                        .reason("Service Unavailable")
                        .request(request())
                        .body("{}", StandardCharsets.UTF_8)
                        .build()
        );
        HttpInsightReputationClient client = new HttpInsightReputationClient(feignClient);

        assertThatThrownBy(() -> client.updatePredictionAccuracy(command()))
                .isInstanceOf(InsightReputationUnavailableException.class);
    }

    @Test
    void updatePredictionAccuracyMapsNullBodyToExternalException() {
        CapturingFeignClient feignClient = new CapturingFeignClient();
        feignClient.response = null;
        HttpInsightReputationClient client = new HttpInsightReputationClient(feignClient);

        assertThatThrownBy(() -> client.updatePredictionAccuracy(command()))
                .isInstanceOf(InsightReputationExternalException.class)
                .hasMessage("INSIGHT_REPUTATION_RESULT_UNKNOWN");
    }

    @Test
    void updatePredictionAccuracyMapsDecodeExceptionToExternalException() {
        CapturingFeignClient feignClient = new CapturingFeignClient();
        feignClient.exception = new DecodeException(200, "decode failed", request());
        HttpInsightReputationClient client = new HttpInsightReputationClient(feignClient);

        assertThatThrownBy(() -> client.updatePredictionAccuracy(command()))
                .isInstanceOf(InsightReputationExternalException.class);
    }

    private PredictionAccuracyUpdateCommand command() {
        return new PredictionAccuracyUpdateCommand(1L, 7L, 123L, true);
    }

    private static <T> InsightApiResponse<T> ok(T data) {
        return new InsightApiResponse<>(true, null, null, data, null);
    }

    private static <T> InsightApiResponse<T> fail(String errorCode, String message) {
        return new InsightApiResponse<>(false, errorCode, message, null, null);
    }

    private Request request() {
        return Request.create(
                Request.HttpMethod.POST,
                "/internal/api/v1/reputations/prediction",
                Map.of(),
                null,
                new RequestTemplate()
        );
    }

    static class CapturingFeignClient implements InsightReputationInternalFeignClient {
        private InsightReputationHttpDtos.PredictionAccuracyUpdateRequest request;
        private InsightApiResponse<InsightReputationHttpDtos.PredictionAccuracyUpdateResponse> response;
        private RuntimeException exception;

        @Override
        public InsightApiResponse<InsightReputationHttpDtos.PredictionAccuracyUpdateResponse> updatePredictionAccuracy(
                InsightReputationHttpDtos.PredictionAccuracyUpdateRequest request
        ) {
            this.request = request;
            if (exception != null) {
                throw exception;
            }
            return response;
        }
    }
}
