package com.todongsan.marketservice.market.service;

import com.todongsan.marketservice.global.exception.CustomException;
import com.todongsan.marketservice.global.exception.errorcode.CommonErrorCode;
import com.todongsan.marketservice.global.exception.errorcode.MarketErrorCode;
import com.todongsan.marketservice.market.dto.response.MyMarketPredictionPageResponse;
import com.todongsan.marketservice.market.dto.response.MyMarketPredictionResponse;
import com.todongsan.marketservice.market.dto.response.MarketPredictionResponse;
import com.todongsan.marketservice.market.entity.MarketPrediction;
import com.todongsan.marketservice.market.repository.MarketMapper;
import com.todongsan.marketservice.market.repository.MyMarketPredictionRow;
import com.todongsan.marketservice.market.type.MarketDisplayStatus;
import com.todongsan.marketservice.market.type.MarketStatus;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MarketPredictionQueryService {

    private final MarketMapper marketMapper;

    @Transactional(readOnly = true)
    public MyMarketPredictionPageResponse getMyPredictions(Long memberId, int page, int size) {
        validateMemberId(memberId);
        int offset = offset(page, size);
        LocalDateTime now = LocalDateTime.now();
        long totalElements = marketMapper.countPredictionsByMemberId(memberId);

        return new MyMarketPredictionPageResponse(
                marketMapper.selectPredictionsByMemberId(memberId, offset, size).stream()
                        .map(row -> toResponse(row, now))
                        .toList(),
                page,
                size,
                totalElements,
                totalPages(totalElements, size),
                isLast(page, size, totalElements)
        );
    }

    @Transactional(readOnly = true)
    public MarketPredictionResponse getMyPrediction(long marketId, Long memberId) {
        validateMemberId(memberId);
        if (marketMapper.selectMarketById(marketId) == null) {
            throw new CustomException(MarketErrorCode.MARKET_NOT_FOUND);
        }

        MarketPrediction prediction = marketMapper.selectPredictionByMarketIdAndMemberId(marketId, memberId);
        if (prediction == null) {
            throw new CustomException(MarketErrorCode.MARKET_PREDICTION_NOT_FOUND);
        }
        return new MarketPredictionResponse(
                prediction.getId(),
                prediction.getMarketId(),
                prediction.getOptionId(),
                prediction.getPointAmount(),
                prediction.getPriceSnapshot(),
                prediction.getContractQuantity(),
                prediction.getStatus(),
                prediction.getCreatedAt(),
                prediction.getUpdatedAt()
        );
    }

    private MyMarketPredictionResponse toResponse(MyMarketPredictionRow row, LocalDateTime now) {
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
                row.getRefundAmount()
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
