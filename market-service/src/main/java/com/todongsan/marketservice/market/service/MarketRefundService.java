package com.todongsan.marketservice.market.service;

import com.todongsan.marketservice.market.client.MemberPointClient;
import com.todongsan.marketservice.market.client.MemberPointRefundBatchRequest;
import com.todongsan.marketservice.market.client.MemberPointRefundBatchResponse;
import com.todongsan.marketservice.market.client.MemberPointRefundItem;
import com.todongsan.marketservice.market.client.exception.MemberPointExternalException;
import com.todongsan.marketservice.market.client.exception.MemberPointTimeoutException;
import com.todongsan.marketservice.market.client.exception.MemberPointUnavailableException;
import com.todongsan.marketservice.market.dto.response.RefundMarketResponse;
import com.todongsan.marketservice.market.entity.MarketRefundDetail;
import com.todongsan.marketservice.market.type.RefundStatus;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MarketRefundService {

    private static final String REFERENCE_TYPE = "MARKET_PREDICTION";
    private static final String REASON = "Market 무효 처리 환불";

    private final MarketRefundTransactionService transactionService;
    private final MemberPointClient memberPointClient;

    public RefundMarketResponse refundMarket(long marketId) {
        MarketRefundPreparation preparation = transactionService.prepareRefund(marketId);
        if (preparation.completed()) {
            return preparation.toResponse(0, 0, 0, RefundStatus.COMPLETED);
        }

        String batchIdempotencyKey = batchIdempotencyKey(preparation);
        MemberPointRefundBatchRequest request = new MemberPointRefundBatchRequest(
                preparation.marketId(),
                batchIdempotencyKey,
                toItems(preparation.refundDetails())
        );
        try {
            MemberPointRefundBatchResponse response = memberPointClient.refundMarketPredictions(
                    batchIdempotencyKey,
                    request
            );
            if (response == null || response.results() == null) {
                return transactionService.applyRefundUnknown(preparation, "MEMBER_POINT_RESULT_UNKNOWN");
            }
            return transactionService.applyRefundResult(preparation, response);
        } catch (MemberPointTimeoutException | MemberPointUnavailableException | MemberPointExternalException e) {
            return transactionService.applyRefundUnknown(preparation, failureReason(e, "MEMBER_POINT_RESULT_UNKNOWN"));
        }
    }

    private List<MemberPointRefundItem> toItems(List<MarketRefundDetail> details) {
        return details.stream()
                .map(detail -> new MemberPointRefundItem(
                        detail.getPredictionId(),
                        detail.getMemberId(),
                        detail.getRefundAmount(),
                        REFERENCE_TYPE,
                        detail.getPredictionId(),
                        REASON,
                        detail.getIdempotencyKey()
                ))
                .toList();
    }

    private String batchIdempotencyKey(MarketRefundPreparation preparation) {
        return "MARKET_REFUND_BATCH:market:%d:void:%d:attempt:1"
                .formatted(preparation.marketId(), preparation.voidId());
    }

    private String failureReason(RuntimeException exception, String fallback) {
        String message = exception.getMessage();
        String reason = message == null || message.isBlank() ? fallback : message;
        return reason.length() <= 255 ? reason : reason.substring(0, 255);
    }
}
