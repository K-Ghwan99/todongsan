package com.todongsan.marketservice.market.service;

import com.todongsan.marketservice.global.exception.CustomException;
import com.todongsan.marketservice.global.exception.errorcode.CommonErrorCode;
import com.todongsan.marketservice.global.exception.errorcode.MarketErrorCode;
import com.todongsan.marketservice.market.dto.response.MyMarketPredictionPageResponse;
import com.todongsan.marketservice.market.dto.response.MyMarketPredictionResponse;
import com.todongsan.marketservice.market.dto.response.MarketPredictionResponse;
import com.todongsan.marketservice.market.repository.MarketMapper;
import com.todongsan.marketservice.market.repository.MyMarketPredictionSearchCondition;
import com.todongsan.marketservice.market.repository.MyMarketPredictionRow;
import com.todongsan.marketservice.market.service.MarketPredictionPayoutEstimateCalculator.PredictionPayoutEstimate;
import com.todongsan.marketservice.market.type.MarketDisplayStatus;
import com.todongsan.marketservice.market.type.MarketStatus;
import com.todongsan.marketservice.market.type.PredictionStatus;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MarketPredictionQueryService {

    private final MarketMapper marketMapper;
    private final MarketPredictionPayoutEstimateCalculator payoutEstimateCalculator;

    @Transactional(readOnly = true)
    public MyMarketPredictionPageResponse getMyPredictions(
            Long memberId,
            int page,
            int size,
            List<String> marketDisplayStatusValues,
            List<String> predictionStatusValues
    ) {
        validateMemberId(memberId);
        List<MarketDisplayStatus> marketDisplayStatuses = parseMarketDisplayStatuses(marketDisplayStatusValues);
        List<PredictionStatus> predictionStatuses = parsePredictionStatuses(predictionStatusValues);
        LocalDateTime now = LocalDateTime.now();
        MyMarketPredictionSearchCondition condition = toCondition(
                memberId,
                offset(page, size),
                size,
                now,
                marketDisplayStatuses,
                predictionStatuses
        );
        long totalElements = marketMapper.countPredictionsByMemberId(condition);

        return new MyMarketPredictionPageResponse(
                marketMapper.selectPredictionsByMemberId(condition).stream()
                        .map(row -> toResponse(row, now))
                        .toList(),
                page,
                size,
                totalElements,
                totalPages(totalElements, size),
                isLast(page, size, totalElements)
        );
    }

    private MyMarketPredictionSearchCondition toCondition(
            Long memberId,
            int offset,
            int size,
            LocalDateTime now,
            List<MarketDisplayStatus> marketDisplayStatuses,
            List<PredictionStatus> predictionStatuses
    ) {
        boolean includeDisplayActive = marketDisplayStatuses.contains(MarketDisplayStatus.ACTIVE);
        boolean includeClosedByTime = marketDisplayStatuses.contains(MarketDisplayStatus.CLOSED_BY_TIME);
        List<MarketStatus> marketStatusesForDisplayFilter = marketDisplayStatuses.stream()
                .filter(status -> status != MarketDisplayStatus.ACTIVE)
                .filter(status -> status != MarketDisplayStatus.CLOSED_BY_TIME)
                .map(status -> MarketStatus.valueOf(status.name()))
                .toList();

        return new MyMarketPredictionSearchCondition(
                memberId,
                offset,
                size,
                now,
                !marketDisplayStatuses.isEmpty(),
                includeDisplayActive,
                includeClosedByTime,
                marketStatusesForDisplayFilter,
                predictionStatuses
        );
    }

    private List<MarketDisplayStatus> parseMarketDisplayStatuses(List<String> values) {
        return parseEnumValues(values, MarketDisplayStatus.class);
    }

    private List<PredictionStatus> parsePredictionStatuses(List<String> values) {
        return parseEnumValues(values, PredictionStatus.class);
    }

    private <E extends Enum<E>> List<E> parseEnumValues(List<String> values, Class<E> enumType) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }

        Set<E> parsed = new LinkedHashSet<>();
        for (String value : values) {
            for (String token : splitValue(value)) {
                try {
                    parsed.add(Enum.valueOf(enumType, token));
                } catch (IllegalArgumentException e) {
                    throw new CustomException(CommonErrorCode.VALIDATION_FAILED);
                }
            }
        }
        return List.copyOf(parsed);
    }

    private List<String> splitValue(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }

        List<String> tokens = new ArrayList<>();
        for (String token : value.split(",")) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                tokens.add(trimmed);
            }
        }
        return tokens;
    }

    @Transactional(readOnly = true)
    public MarketPredictionResponse getMyPrediction(long marketId, Long memberId) {
        validateMemberId(memberId);
        if (marketMapper.selectMarketById(marketId) == null) {
            throw new CustomException(MarketErrorCode.MARKET_NOT_FOUND);
        }

        MyMarketPredictionRow row = marketMapper.selectPredictionForPayoutEstimate(marketId, memberId);
        if (row == null) {
            throw new CustomException(MarketErrorCode.MARKET_PREDICTION_NOT_FOUND);
        }
        PredictionPayoutEstimate estimate = estimate(row);
        return new MarketPredictionResponse(
                row.getPredictionId(),
                row.getMarketId(),
                row.getSelectedOptionId(),
                row.getPointAmount(),
                row.getPriceSnapshot(),
                row.getContractQuantity(),
                row.getPredictionStatus(),
                row.getCreatedAt(),
                row.getUpdatedAt(),
                estimate.estimatedPayoutIfWin(),
                estimate.estimatedProfitIfWin(),
                estimate.estimatedProfitRateIfWin(),
                estimate.currentPayoutPerContract(),
                estimate.estimateBaseTotalPool(),
                estimate.estimateBaseSettlementPool(),
                estimate.estimateBaseOptionContractQuantity()
        );
    }

    private MyMarketPredictionResponse toResponse(MyMarketPredictionRow row, LocalDateTime now) {
        PredictionPayoutEstimate estimate = estimate(row);
        return new MyMarketPredictionResponse(
                row.getPredictionId(),
                row.getMarketId(),
                row.getMarketTitle(),
                row.getMarketStatus(),
                displayStatus(row.getMarketStatus(), row.getCloseAt(), now),
                canPredict(row.getMarketStatus(), row.getCloseAt(), now),
                row.getSelectedOptionId(),
                row.getSelectedOptionContent(),
                row.getPointAmount(),
                row.getPriceSnapshot(),
                row.getContractQuantity(),
                row.getPredictionStatus(),
                row.getCreatedAt(),
                row.getUpdatedAt(),
                row.getCloseAt(),
                row.getSettledAmount(),
                row.getRefundAmount(),
                estimate.estimatedPayoutIfWin(),
                estimate.estimatedProfitIfWin(),
                estimate.estimatedProfitRateIfWin(),
                estimate.currentPayoutPerContract(),
                estimate.estimateBaseTotalPool(),
                estimate.estimateBaseSettlementPool(),
                estimate.estimateBaseOptionContractQuantity()
        );
    }

    private PredictionPayoutEstimate estimate(MyMarketPredictionRow row) {
        return payoutEstimateCalculator.calculate(
                row.getPredictionStatus(),
                row.getPointAmount(),
                row.getContractQuantity(),
                row.getFeeRate(),
                row.getEstimateBaseTotalPool(),
                row.getEstimateBaseOptionPointAmount(),
                row.getEstimateBaseOptionContractQuantity()
        );
    }

    private boolean canPredict(MarketStatus status, LocalDateTime closeAt, LocalDateTime now) {
        return status == MarketStatus.ACTIVE && closeAt.isAfter(now);
    }

    private MarketDisplayStatus displayStatus(MarketStatus status, LocalDateTime closeAt, LocalDateTime now) {
        if (status == MarketStatus.ACTIVE && !closeAt.isAfter(now)) {
            return MarketDisplayStatus.CLOSED_BY_TIME;
        }
        return MarketDisplayStatus.valueOf(status.name());
    }

    private void validateMemberId(Long memberId) {
        if (memberId == null || memberId <= 0) {
            throw new CustomException(CommonErrorCode.VALIDATION_FAILED);
        }
    }

    private int offset(int page, int size) {
        long offset = (long) page * size;
        if (offset > Integer.MAX_VALUE) {
            throw new CustomException(CommonErrorCode.VALIDATION_FAILED);
        }
        return (int) offset;
    }

    private int totalPages(long totalElements, int size) {
        return (int) ((totalElements + size - 1) / size);
    }

    private boolean isLast(int page, int size, long totalElements) {
        return (long) (page + 1) * size >= totalElements;
    }
}
