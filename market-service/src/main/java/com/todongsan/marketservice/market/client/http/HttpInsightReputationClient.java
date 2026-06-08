package com.todongsan.marketservice.market.client.http;

import com.todongsan.marketservice.market.client.InsightReputationClient;
import com.todongsan.marketservice.market.client.PredictionAccuracyUpdateCommand;
import com.todongsan.marketservice.market.client.PredictionAccuracyUpdateResult;
import com.todongsan.marketservice.market.client.exception.InsightReputationExternalException;
import com.todongsan.marketservice.market.client.exception.InsightReputationFailedException;
import com.todongsan.marketservice.market.client.exception.InsightReputationTimeoutException;
import com.todongsan.marketservice.market.client.exception.InsightReputationUnavailableException;
import com.todongsan.marketservice.market.client.http.InsightReputationHttpDtos.PredictionAccuracyUpdateResponse;
import feign.FeignException;
import feign.RetryableException;
import feign.codec.DecodeException;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "client.insight.mode",
        havingValue = "http"
)
public class HttpInsightReputationClient implements InsightReputationClient {

    private static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
    private static final String VALIDATION_FAILED = "VALIDATION_FAILED";
    private static final String INSIGHT_REPUTATION_RESULT_UNKNOWN = "INSIGHT_REPUTATION_RESULT_UNKNOWN";
    private static final String INSIGHT_REPUTATION_TIMEOUT = "INSIGHT_REPUTATION_TIMEOUT";
    private static final String INSIGHT_REPUTATION_UNAVAILABLE = "INSIGHT_REPUTATION_UNAVAILABLE";
    private static final Pattern ERROR_CODE_PATTERN = Pattern.compile("\"errorCode\"\\s*:\\s*\"([^\"]+)\"");

    private final InsightReputationInternalFeignClient feignClient;

    @Override
    public PredictionAccuracyUpdateResult updatePredictionAccuracy(PredictionAccuracyUpdateCommand command) {
        PredictionAccuracyUpdateResponse response = unwrap(call(() -> feignClient.updatePredictionAccuracy(
                InsightReputationHttpDtos.PredictionAccuracyUpdateRequest.from(command)
        )));
        return new PredictionAccuracyUpdateResult(
                response.memberId(),
                response.predictionCount(),
                response.predictionCorrect(),
                response.predictionAccuracy()
        );
    }

    private <T> T call(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (RetryableException e) {
            throw new InsightReputationTimeoutException(failureMessage(e, INSIGHT_REPUTATION_TIMEOUT));
        } catch (DecodeException e) {
            throw new InsightReputationExternalException(failureMessage(e, INSIGHT_REPUTATION_RESULT_UNKNOWN));
        } catch (FeignException e) {
            throw mapFeignException(e);
        }
    }

    private <T> T unwrap(InsightApiResponse<T> response) {
        if (response == null) {
            throw new InsightReputationExternalException(INSIGHT_REPUTATION_RESULT_UNKNOWN);
        }
        if (!Boolean.TRUE.equals(response.success())) {
            throw mapErrorCode(response.errorCode(), response.message());
        }
        if (response.data() == null) {
            throw new InsightReputationExternalException(INSIGHT_REPUTATION_RESULT_UNKNOWN);
        }
        return response.data();
    }

    private RuntimeException mapFeignException(FeignException e) {
        String errorCode = extractErrorCode(e.contentUTF8());
        if (isKnownFailure(errorCode)) {
            return new InsightReputationFailedException(errorCode);
        }
        if (e.status() >= 500) {
            return new InsightReputationUnavailableException(failureMessage(e, INSIGHT_REPUTATION_UNAVAILABLE));
        }
        return new InsightReputationExternalException(errorCode == null
                ? failureMessage(e, INSIGHT_REPUTATION_RESULT_UNKNOWN)
                : errorCode);
    }

    private RuntimeException mapErrorCode(String errorCode, String message) {
        if (isKnownFailure(errorCode)) {
            return new InsightReputationFailedException(errorCode);
        }
        return new InsightReputationExternalException(failureMessage(message, errorCode, INSIGHT_REPUTATION_RESULT_UNKNOWN));
    }

    private boolean isKnownFailure(String errorCode) {
        return RESOURCE_NOT_FOUND.equals(errorCode)
                || VALIDATION_FAILED.equals(errorCode);
    }

    private String extractErrorCode(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        Matcher matcher = ERROR_CODE_PATTERN.matcher(body);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String failureMessage(RuntimeException exception, String fallback) {
        return failureMessage(exception.getMessage(), null, fallback);
    }

    private String failureMessage(String message, String errorCode, String fallback) {
        String value = message == null || message.isBlank() ? errorCode : message;
        if (value == null || value.isBlank()) {
            value = fallback;
        }
        return value.length() <= 255 ? value : value.substring(0, 255);
    }
}
