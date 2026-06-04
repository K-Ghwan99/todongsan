package com.todongsan.marketservice.market.service;

import com.todongsan.marketservice.market.client.MemberPointClient;
import com.todongsan.marketservice.market.client.MemberPointSettlementBatchRequest;
import com.todongsan.marketservice.market.client.MemberPointSettlementBatchResponse;
import com.todongsan.marketservice.market.client.MemberPointSettlementItem;
import com.todongsan.marketservice.market.client.exception.MemberPointExternalException;
import com.todongsan.marketservice.market.client.exception.MemberPointTimeoutException;
import com.todongsan.marketservice.market.client.exception.MemberPointUnavailableException;
import com.todongsan.marketservice.market.dto.response.SettleMarketResponse;
import com.todongsan.marketservice.market.entity.MarketSettlementDetail;
import com.todongsan.marketservice.market.type.MarketStatus;
import com.todongsan.marketservice.market.type.SettlementStatus;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MarketSettlementService {

    private static final String REFERENCE_TYPE = "MARKET_PREDICTION";
    private static final String REASON = "Market 정산 보상";

    private final MarketSettlementTransactionService transactionService;
    private final MemberPointClient memberPointClient;

    public SettleMarketResponse settleMarket(long marketId) {
        SettlementPreparation preparation = transactionService.prepareSettlement(marketId);
        if (preparation.completed()) {
            return preparation.toResponse(
                    MarketStatus.SETTLED,
                    SettlementStatus.COMPLETED,
                    0,
                    0
            );
        }

        String batchIdempotencyKey = batchIdempotencyKey(preparation);
        MemberPointSettlementBatchRequest request = new MemberPointSettlementBatchRequest(
                preparation.marketId(),
                batchIdempotencyKey,
                toItems(preparation.winnerDetails())
        );
        try {
            MemberPointSettlementBatchResponse response = memberPointClient.settleMarketRewards(
                    batchIdempotencyKey,
                    request
            );
            if (response == null || response.results() == null) {
                return transactionService.applySettlementUnknown(preparation, "MEMBER_POINT_RESULT_UNKNOWN");
            }
            return transactionService.applySettlementResult(preparation, response);
        } catch (MemberPointTimeoutException | MemberPointUnavailableException | MemberPointExternalException e) {
            return transactionService.applySettlementUnknown(preparation, failureReason(e, "MEMBER_POINT_RESULT_UNKNOWN"));
        }
    }

    private List<MemberPointSettlementItem> toItems(List<MarketSettlementDetail> details) {
        return details.stream()
                .map(detail -> new MemberPointSettlementItem(
                        detail.getPredictionId(),
                        detail.getMemberId(),
                        detail.getSettledAmount(),
                        REFERENCE_TYPE,
                        detail.getPredictionId(),
                        REASON,
                        detail.getIdempotencyKey()
                ))
                .toList();
    }

    private String batchIdempotencyKey(SettlementPreparation preparation) {
        return "MARKET_SETTLEMENT_BATCH:market:%d:settlement:%d:attempt:1"
                .formatted(preparation.marketId(), preparation.settlementId());
    }

    private String failureReason(RuntimeException exception, String fallback) {
        String message = exception.getMessage();
        String reason = message == null || message.isBlank() ? fallback : message;
        return reason.length() <= 255 ? reason : reason.substring(0, 255);
    }
}
