package com.todongsan.marketservice.market.service;

import com.todongsan.marketservice.global.exception.CustomException;
import com.todongsan.marketservice.global.exception.errorcode.CommonErrorCode;
import com.todongsan.marketservice.global.exception.errorcode.MarketErrorCode;
import com.todongsan.marketservice.market.client.MemberPointClient;
import com.todongsan.marketservice.market.client.PointSpendCommand;
import com.todongsan.marketservice.market.dto.request.CreatePredictionRequest;
import com.todongsan.marketservice.market.dto.response.CreatePredictionResponse;
import com.todongsan.marketservice.market.entity.MarketPrediction;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MarketPredictionService {

    private static final String SPEND_TYPE = "SPEND_MARKET";
    private static final String REFERENCE_TYPE = "MARKET_PREDICTION";

    private final MarketPredictionTransactionService transactionService;
    private final MemberPointClient memberPointClient;

    public CreatePredictionResponse createPrediction(
            long marketId,
            Long memberId,
            String idempotencyKey,
            CreatePredictionRequest request
    ) {
        validateHeaders(marketId, memberId, idempotencyKey);

        MarketPrediction prediction;
        try {
            prediction = transactionService.createPendingPrediction(
                    marketId,
                    memberId,
                    idempotencyKey,
                    request
            );
        } catch (DuplicateKeyException e) {
            throw new CustomException(MarketErrorCode.MARKET_ALREADY_PREDICTED);
        }
        memberPointClient.spend(new PointSpendCommand(
                memberId,
                SPEND_TYPE,
                prediction.getPointAmount(),
                REFERENCE_TYPE,
                prediction.getId(),
                idempotencyKey
        ));
        return transactionService.confirmPrediction(prediction.getId());
    }

    private void validateHeaders(long marketId, Long memberId, String idempotencyKey) {
        if (memberId == null || memberId <= 0) {
            throw new CustomException(CommonErrorCode.VALIDATION_FAILED);
        }
        String expectedIdempotencyKey = "MARKET_PREDICTION_SPEND:market:%d:member:%d"
                .formatted(marketId, memberId);
        if (!expectedIdempotencyKey.equals(idempotencyKey)) {
            throw new CustomException(CommonErrorCode.VALIDATION_FAILED);
        }
    }
}
