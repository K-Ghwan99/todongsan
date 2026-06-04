package com.todongsan.marketservice.market.service;

import com.todongsan.marketservice.global.exception.CustomException;
import com.todongsan.marketservice.global.exception.errorcode.MarketErrorCode;
import com.todongsan.marketservice.market.client.MemberPointRefundBatchResponse;
import com.todongsan.marketservice.market.client.MemberPointRefundItemResult;
import com.todongsan.marketservice.market.client.MemberPointRefundItemStatus;
import com.todongsan.marketservice.market.dto.response.RefundMarketResponse;
import com.todongsan.marketservice.market.entity.Market;
import com.todongsan.marketservice.market.entity.MarketPrediction;
import com.todongsan.marketservice.market.entity.MarketRefundDetail;
import com.todongsan.marketservice.market.entity.MarketVoid;
import com.todongsan.marketservice.market.repository.MarketMapper;
import com.todongsan.marketservice.market.type.MarketStatus;
import com.todongsan.marketservice.market.type.RefundStatus;
import com.todongsan.marketservice.market.type.TransactionItemStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MarketRefundTransactionService {

    private final MarketMapper marketMapper;

    @Transactional
    public MarketRefundPreparation prepareRefund(long marketId) {
        LocalDateTime now = LocalDateTime.now();
        Market market = marketMapper.lockMarketById(marketId);
        if (market == null) {
            throw new CustomException(MarketErrorCode.MARKET_NOT_FOUND);
        }
        if (market.getStatus() != MarketStatus.VOIDED) {
            throw new CustomException(MarketErrorCode.MARKET_INVALID_STATUS);
        }

        MarketVoid marketVoid = marketMapper.selectMarketVoidByMarketId(marketId);
        if (marketVoid == null) {
            throw new CustomException(MarketErrorCode.MARKET_INVALID_SETTLEMENT_DATA);
        }
        if (marketMapper.countRefundDetailsByVoidId(marketVoid.getId()) > 0) {
            throw new CustomException(MarketErrorCode.MARKET_ALREADY_REFUNDED);
        }

        List<MarketPrediction> targets = marketMapper.selectConfirmedPredictionsForRefund(marketId);
        if (targets.isEmpty()) {
            marketMapper.updateMarketVoidRefundStatus(marketVoid.getId(), RefundStatus.COMPLETED, now);
            return new MarketRefundPreparation(marketId, marketVoid.getId(), List.of(), true);
        }

        List<MarketRefundDetail> details = targets.stream()
                .map(prediction -> toPendingDetail(marketId, marketVoid.getId(), prediction, now))
                .toList();
        for (MarketRefundDetail detail : details) {
            if (marketMapper.updatePredictionToRefundPending(detail.getPredictionId(), now) != 1) {
                throw new CustomException(MarketErrorCode.MARKET_INVALID_SETTLEMENT_DATA);
            }
        }
        marketMapper.insertMarketRefundDetails(details);
        marketMapper.updateMarketVoidRefundStatus(marketVoid.getId(), RefundStatus.IN_PROGRESS, now);

        List<MarketRefundDetail> savedDetails = marketMapper.selectPendingRefundDetailsByVoidId(marketVoid.getId());
        return new MarketRefundPreparation(marketId, marketVoid.getId(), savedDetails, false);
    }

    @Transactional
    public RefundMarketResponse applyRefundResult(
            MarketRefundPreparation preparation,
            MemberPointRefundBatchResponse response
    ) {
        LocalDateTime now = LocalDateTime.now();
        Map<Long, MemberPointRefundItemResult> resultsByPredictionId = response.results().stream()
                .collect(Collectors.toMap(
                        MemberPointRefundItemResult::predictionId,
                        Function.identity(),
                        (first, ignored) -> first
                ));

        int successCount = 0;
        int failedCount = 0;
        int unknownCount = 0;
        for (MarketRefundDetail detail : preparation.refundDetails()) {
            MemberPointRefundItemResult result = resultsByPredictionId.get(detail.getPredictionId());
            if (result == null) {
                markRefundUnknown(detail, "MEMBER_POINT_RESULT_MISSING", now);
                unknownCount++;
                continue;
            }
            if (isSuccess(result.status())) {
                markRefundSuccess(detail, now);
                successCount++;
                continue;
            }
            markRefundFailed(detail, failureReason(result.errorCode(), "MEMBER_POINT_FAILED"), now);
            failedCount++;
        }

        RefundStatus refundStatus = failedCount == 0 && unknownCount == 0
                ? RefundStatus.COMPLETED
                : RefundStatus.IN_PROGRESS;
        marketMapper.updateMarketVoidRefundStatus(preparation.voidId(), refundStatus, now);
        return preparation.toResponse(successCount, failedCount, unknownCount, refundStatus);
    }

    @Transactional
    public RefundMarketResponse applyRefundUnknown(MarketRefundPreparation preparation, String failReason) {
        LocalDateTime now = LocalDateTime.now();
        for (MarketRefundDetail detail : preparation.refundDetails()) {
            markRefundUnknown(detail, failReason, now);
        }
        marketMapper.updateMarketVoidRefundStatus(preparation.voidId(), RefundStatus.IN_PROGRESS, now);
        return preparation.toResponse(0, 0, preparation.refundDetails().size(), RefundStatus.IN_PROGRESS);
    }

    private MarketRefundDetail toPendingDetail(
            long marketId,
            Long voidId,
            MarketPrediction prediction,
            LocalDateTime now
    ) {
        MarketRefundDetail detail = new MarketRefundDetail();
        detail.setMarketVoidId(voidId);
        detail.setPredictionId(prediction.getId());
        detail.setMemberId(prediction.getMemberId());
        detail.setRefundAmount(prediction.getPointAmount());
        detail.setStatus(TransactionItemStatus.PENDING);
        detail.setIdempotencyKey(refundIdempotencyKey(marketId, prediction));
        detail.setCreatedAt(now);
        detail.setUpdatedAt(now);
        return detail;
    }

    private void markRefundSuccess(MarketRefundDetail detail, LocalDateTime now) {
        marketMapper.updateRefundDetailStatus(detail.getId(), TransactionItemStatus.SUCCESS, null, now);
        marketMapper.markPredictionRefunded(detail.getPredictionId(), detail.getRefundAmount(), now);
    }

    private void markRefundFailed(MarketRefundDetail detail, String failReason, LocalDateTime now) {
        marketMapper.updateRefundDetailStatus(detail.getId(), TransactionItemStatus.FAILED, failReason, now);
    }

    private void markRefundUnknown(MarketRefundDetail detail, String failReason, LocalDateTime now) {
        marketMapper.updateRefundDetailStatus(detail.getId(), TransactionItemStatus.UNKNOWN, failReason, now);
        marketMapper.markPredictionRefundUnknown(detail.getPredictionId(), failReason, now);
    }

    private String refundIdempotencyKey(long marketId, MarketPrediction prediction) {
        return "MARKET_REFUND:market:%d:prediction:%d:member:%d"
                .formatted(marketId, prediction.getId(), prediction.getMemberId());
    }

    private boolean isSuccess(MemberPointRefundItemStatus status) {
        return status == MemberPointRefundItemStatus.PROCESSED
                || status == MemberPointRefundItemStatus.ALREADY_PROCESSED;
    }

    private String failureReason(String reason, String fallback) {
        String value = reason == null || reason.isBlank() ? fallback : reason;
        return value.length() <= 255 ? value : value.substring(0, 255);
    }
}
