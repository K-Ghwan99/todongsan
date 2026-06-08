package com.todongsan.marketservice.market.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.todongsan.marketservice.market.client.InsightReputationClient;
import com.todongsan.marketservice.market.client.PredictionAccuracyUpdateCommand;
import com.todongsan.marketservice.market.client.PredictionAccuracyUpdateResult;
import com.todongsan.marketservice.market.client.exception.InsightReputationExternalException;
import com.todongsan.marketservice.market.client.exception.InsightReputationFailedException;
import com.todongsan.marketservice.market.client.exception.InsightReputationTimeoutException;
import com.todongsan.marketservice.market.client.exception.InsightReputationUnavailableException;
import com.todongsan.marketservice.market.repository.MarketMapper;
import com.todongsan.marketservice.market.repository.MarketReputationUpdateRow;
import com.todongsan.marketservice.market.type.ReputationUpdateStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MarketReputationUpdateServiceTest {

    @Mock
    private MarketMapper marketMapper;

    @Mock
    private InsightReputationClient insightReputationClient;

    @Test
    void processPendingTaskUpdatesInsightAndMarksSuccess() {
        MarketReputationUpdateService service = service();
        MarketReputationUpdateRow row = row(1L, 10L, 100L, 1000L, true, ReputationUpdateStatus.PENDING);
        when(marketMapper.selectPendingOrUnknownReputationUpdates(50)).thenReturn(List.of(row));
        when(insightReputationClient.updatePredictionAccuracy(any()))
                .thenReturn(result(1000L));

        int processedCount = service.processPendingOrUnknownUpdates(50);

        assertThat(processedCount).isEqualTo(1);
        ArgumentCaptor<PredictionAccuracyUpdateCommand> captor =
                ArgumentCaptor.forClass(PredictionAccuracyUpdateCommand.class);
        verify(insightReputationClient).updatePredictionAccuracy(captor.capture());
        assertThat(captor.getValue().memberId()).isEqualTo(1000L);
        assertThat(captor.getValue().marketId()).isEqualTo(10L);
        assertThat(captor.getValue().predictionId()).isEqualTo(100L);
        assertThat(captor.getValue().isCorrect()).isTrue();
        verify(marketMapper).markReputationUpdateSuccess(eq(1L), any(LocalDateTime.class));
    }

    @Test
    void processUnknownTaskCanRecoverToSuccess() {
        MarketReputationUpdateService service = service();
        MarketReputationUpdateRow row = row(2L, 10L, 101L, 1001L, false, ReputationUpdateStatus.UNKNOWN);
        when(marketMapper.selectPendingOrUnknownReputationUpdates(50)).thenReturn(List.of(row));
        when(insightReputationClient.updatePredictionAccuracy(any()))
                .thenReturn(result(1001L));

        int processedCount = service.processPendingOrUnknownUpdates(50);

        assertThat(processedCount).isEqualTo(1);
        verify(marketMapper).markReputationUpdateSuccess(eq(2L), any(LocalDateTime.class));
    }

    @Test
    void resourceNotFoundFailureMarksFailed() {
        MarketReputationUpdateService service = service();
        MarketReputationUpdateRow row = row(1L, 10L, 100L, 1000L, true, ReputationUpdateStatus.PENDING);
        when(marketMapper.selectPendingOrUnknownReputationUpdates(50)).thenReturn(List.of(row));
        when(insightReputationClient.updatePredictionAccuracy(any()))
                .thenThrow(new InsightReputationFailedException("RESOURCE_NOT_FOUND"));

        service.processPendingOrUnknownUpdates(50);

        verify(marketMapper).markReputationUpdateFailed(
                eq(1L),
                eq("RESOURCE_NOT_FOUND"),
                eq("RESOURCE_NOT_FOUND"),
                any(LocalDateTime.class)
        );
    }

    @Test
    void validationFailedFailureMarksFailed() {
        MarketReputationUpdateService service = service();
        MarketReputationUpdateRow row = row(1L, 10L, 100L, 1000L, true, ReputationUpdateStatus.PENDING);
        when(marketMapper.selectPendingOrUnknownReputationUpdates(50)).thenReturn(List.of(row));
        when(insightReputationClient.updatePredictionAccuracy(any()))
                .thenThrow(new InsightReputationFailedException("VALIDATION_FAILED"));

        service.processPendingOrUnknownUpdates(50);

        verify(marketMapper).markReputationUpdateFailed(
                eq(1L),
                eq("VALIDATION_FAILED"),
                eq("VALIDATION_FAILED"),
                any(LocalDateTime.class)
        );
    }

    @Test
    void timeoutMarksUnknown() {
        MarketReputationUpdateService service = service();
        MarketReputationUpdateRow row = row(1L, 10L, 100L, 1000L, true, ReputationUpdateStatus.PENDING);
        when(marketMapper.selectPendingOrUnknownReputationUpdates(50)).thenReturn(List.of(row));
        when(insightReputationClient.updatePredictionAccuracy(any()))
                .thenThrow(new InsightReputationTimeoutException("timeout"));

        service.processPendingOrUnknownUpdates(50);

        verify(marketMapper).markReputationUpdateUnknown(
                eq(1L),
                eq("INSIGHT_REPUTATION_TIMEOUT"),
                eq("timeout"),
                any(LocalDateTime.class)
        );
    }

    @Test
    void unavailableMarksUnknown() {
        MarketReputationUpdateService service = service();
        MarketReputationUpdateRow row = row(1L, 10L, 100L, 1000L, true, ReputationUpdateStatus.PENDING);
        when(marketMapper.selectPendingOrUnknownReputationUpdates(50)).thenReturn(List.of(row));
        when(insightReputationClient.updatePredictionAccuracy(any()))
                .thenThrow(new InsightReputationUnavailableException("unavailable"));

        service.processPendingOrUnknownUpdates(50);

        verify(marketMapper).markReputationUpdateUnknown(
                eq(1L),
                eq("INSIGHT_REPUTATION_UNAVAILABLE"),
                eq("unavailable"),
                any(LocalDateTime.class)
        );
    }

    @Test
    void externalErrorMarksUnknown() {
        MarketReputationUpdateService service = service();
        MarketReputationUpdateRow row = row(1L, 10L, 100L, 1000L, true, ReputationUpdateStatus.PENDING);
        when(marketMapper.selectPendingOrUnknownReputationUpdates(50)).thenReturn(List.of(row));
        when(insightReputationClient.updatePredictionAccuracy(any()))
                .thenThrow(new InsightReputationExternalException("bad response"));

        service.processPendingOrUnknownUpdates(50);

        verify(marketMapper).markReputationUpdateUnknown(
                eq(1L),
                eq("INSIGHT_REPUTATION_EXTERNAL"),
                eq("bad response"),
                any(LocalDateTime.class)
        );
    }

    @Test
    void unexpectedRuntimeExceptionMarksUnknown() {
        MarketReputationUpdateService service = service();
        MarketReputationUpdateRow row = row(1L, 10L, 100L, 1000L, true, ReputationUpdateStatus.PENDING);
        when(marketMapper.selectPendingOrUnknownReputationUpdates(50)).thenReturn(List.of(row));
        when(insightReputationClient.updatePredictionAccuracy(any()))
                .thenThrow(new IllegalStateException("boom"));

        service.processPendingOrUnknownUpdates(50);

        verify(marketMapper).markReputationUpdateUnknown(
                eq(1L),
                eq("INSIGHT_REPUTATION_UNKNOWN"),
                eq("boom"),
                any(LocalDateTime.class)
        );
    }

    @Test
    void oneTaskFailureDoesNotStopRemainingTasks() {
        MarketReputationUpdateService service = service();
        MarketReputationUpdateRow first = row(1L, 10L, 100L, 1000L, true, ReputationUpdateStatus.PENDING);
        MarketReputationUpdateRow second = row(2L, 10L, 101L, 1001L, false, ReputationUpdateStatus.PENDING);
        MarketReputationUpdateRow third = row(3L, 10L, 102L, 1002L, true, ReputationUpdateStatus.UNKNOWN);
        when(marketMapper.selectPendingOrUnknownReputationUpdates(50)).thenReturn(List.of(first, second, third));
        when(insightReputationClient.updatePredictionAccuracy(any()))
                .thenReturn(result(1000L))
                .thenThrow(new InsightReputationUnavailableException("unavailable"))
                .thenReturn(result(1002L));

        int processedCount = service.processPendingOrUnknownUpdates(50);

        assertThat(processedCount).isEqualTo(3);
        verify(marketMapper).markReputationUpdateSuccess(eq(1L), any(LocalDateTime.class));
        verify(marketMapper).markReputationUpdateUnknown(
                eq(2L),
                eq("INSIGHT_REPUTATION_UNAVAILABLE"),
                eq("unavailable"),
                any(LocalDateTime.class)
        );
        verify(marketMapper).markReputationUpdateSuccess(eq(3L), any(LocalDateTime.class));
    }

    @Test
    void noSelectedTasksDoesNotCallInsightClient() {
        MarketReputationUpdateService service = service();
        when(marketMapper.selectPendingOrUnknownReputationUpdates(50)).thenReturn(List.of());

        int processedCount = service.processPendingOrUnknownUpdates(50);

        assertThat(processedCount).isZero();
        verify(insightReputationClient, never()).updatePredictionAccuracy(any());
    }

    private MarketReputationUpdateService service() {
        return new MarketReputationUpdateService(marketMapper, insightReputationClient);
    }

    private MarketReputationUpdateRow row(
            Long id,
            Long marketId,
            Long predictionId,
            Long memberId,
            Boolean isCorrect,
            ReputationUpdateStatus status
    ) {
        LocalDateTime now = LocalDateTime.of(2026, 6, 2, 15, 30);
        return new MarketReputationUpdateRow(
                id,
                marketId,
                predictionId,
                memberId,
                isCorrect,
                status,
                0,
                null,
                null,
                now,
                now
        );
    }

    private PredictionAccuracyUpdateResult result(Long memberId) {
        return new PredictionAccuracyUpdateResult(memberId, 1, 1, BigDecimal.valueOf(100));
    }
}
