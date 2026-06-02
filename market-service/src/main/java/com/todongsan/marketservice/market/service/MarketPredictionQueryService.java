package com.todongsan.marketservice.market.service;

import com.todongsan.marketservice.global.exception.CustomException;
import com.todongsan.marketservice.global.exception.errorcode.CommonErrorCode;
import com.todongsan.marketservice.global.exception.errorcode.MarketErrorCode;
import com.todongsan.marketservice.market.dto.response.MarketPredictionResponse;
import com.todongsan.marketservice.market.entity.MarketPrediction;
import com.todongsan.marketservice.market.repository.MarketMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MarketPredictionQueryService {

    private final MarketMapper marketMapper;

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

    private void validateMemberId(Long memberId) {
        if (memberId == null || memberId <= 0) {
            throw new CustomException(CommonErrorCode.VALIDATION_FAILED);
        }
    }
}
