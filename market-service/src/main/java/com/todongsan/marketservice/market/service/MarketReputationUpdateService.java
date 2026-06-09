package com.todongsan.marketservice.market.service;

import com.todongsan.marketservice.market.client.InsightReputationClient;
import com.todongsan.marketservice.market.client.PredictionAccuracyUpdateCommand;
import com.todongsan.marketservice.market.client.exception.InsightReputationExternalException;
import com.todongsan.marketservice.market.client.exception.InsightReputationFailedException;
import com.todongsan.marketservice.market.client.exception.InsightReputationTimeoutException;
import com.todongsan.marketservice.market.client.exception.InsightReputationUnavailableException;
import com.todongsan.marketservice.market.repository.MarketMapper;
import com.todongsan.marketservice.market.repository.MarketReputationUpdateRow;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketReputationUpdateService {

    private static final int ERROR_CODE_MAX_LENGTH = 100;
    private static final int ERROR_MESSAGE_MAX_LENGTH = 500;
    private static final String INSIGHT_REPUTATION_TIMEOUT = "INSIGHT_REPUTATION_TIMEOUT";
    private static final String INSIGHT_REPUTATION_UNAVAILABLE = "INSIGHT_REPUTATION_UNAVAILABLE";
    private static final String INSIGHT_REPUTATION_EXTERNAL = "INSIGHT_REPUTATION_EXTERNAL";
    private static final String INSIGHT_REPUTATION_UNKNOWN = "INSIGHT_REPUTATION_UNKNOWN";

    private final MarketMapper marketMapper;
    private final InsightReputationClient insightReputationClient;

    public int processPendingOrUnknownUpdates(int limit) {
        List<MarketReputationUpdateRow> rows = marketMapper.selectPendingOrUnknownReputationUpdates(limit);
        int processedCount = 0;
        for (MarketReputationUpdateRow row : rows) {
            process(row);
            processedCount++;
        }
        return processedCount;
    }

    private void process(MarketReputationUpdateRow row) {
        try {
            insightReputationClient.updatePredictionAccuracy(toCommand(row));
            marketMapper.markReputationUpdateSuccess(row.getId(), LocalDateTime.now());
        } catch (InsightReputationFailedException e) {
            markFailed(row, e);
        } catch (InsightReputationTimeoutException e) {
            markUnknown(row, INSIGHT_REPUTATION_TIMEOUT, e);
        } catch (InsightReputationUnavailableException e) {
            markUnknown(row, INSIGHT_REPUTATION_UNAVAILABLE, e);
        } catch (InsightReputationExternalException e) {
            markUnknown(row, INSIGHT_REPUTATION_EXTERNAL, e);
        } catch (RuntimeException e) {
            markUnknown(row, INSIGHT_REPUTATION_UNKNOWN, e);
        }
    }

    private PredictionAccuracyUpdateCommand toCommand(MarketReputationUpdateRow row) {
        return new PredictionAccuracyUpdateCommand(
                row.getMemberId(),
                row.getMarketId(),
                row.getPredictionId(),
                row.getIsCorrect()
        );
    }

    private void markFailed(MarketReputationUpdateRow row, RuntimeException e) {
        String errorCode = truncated(nonBlank(e.getMessage(), e.getClass().getSimpleName()), ERROR_CODE_MAX_LENGTH);
        String errorMessage = truncated(nonBlank(e.getMessage(), errorCode), ERROR_MESSAGE_MAX_LENGTH);
        marketMapper.markReputationUpdateFailed(row.getId(), errorCode, errorMessage, LocalDateTime.now());
        log.info("Reputation update task failed. taskId={}, errorCode={}", row.getId(), errorCode);
    }

    private void markUnknown(MarketReputationUpdateRow row, String errorCode, RuntimeException e) {
        String errorMessage = truncated(nonBlank(e.getMessage(), errorCode), ERROR_MESSAGE_MAX_LENGTH);
        marketMapper.markReputationUpdateUnknown(row.getId(), errorCode, errorMessage, LocalDateTime.now());
        log.warn("Reputation update task result is unknown. taskId={}, errorCode={}", row.getId(), errorCode, e);
    }

    private String nonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String truncated(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
