package com.todongsan.marketservice.market.client.http;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.todongsan.marketservice.global.serializer.BigDecimalPlainStringSerializer;
import com.todongsan.marketservice.market.client.MemberPointRefundBatchRequest;
import com.todongsan.marketservice.market.client.MemberPointRefundItem;
import com.todongsan.marketservice.market.client.MemberPointSettlementBatchRequest;
import com.todongsan.marketservice.market.client.MemberPointSettlementItem;
import com.todongsan.marketservice.market.client.PointSpendCommand;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class MemberPointHttpDtos {

    private MemberPointHttpDtos() {
    }

    public record SpendRequest(
            Long memberId,
            String type,
            @JsonSerialize(using = BigDecimalPlainStringSerializer.class)
            BigDecimal amount,
            String referenceType,
            Long referenceId,
            String reason
    ) {
        static SpendRequest from(PointSpendCommand command, String reason) {
            return new SpendRequest(
                    command.memberId(),
                    command.type(),
                    command.amount(),
                    command.referenceType(),
                    command.referenceId(),
                    reason
            );
        }
    }

    public record SpendResponse(
            Long memberId,
            String type,
            BigDecimal amount,
            String referenceType,
            Long referenceId,
            BigDecimal balanceSnapshot
    ) {
    }

    public record TransactionStatusResponse(
            String idempotencyKey,
            String status,
            Long memberId,
            String type,
            BigDecimal amount,
            String referenceType,
            Long referenceId,
            BigDecimal balanceSnapshot,
            LocalDateTime createdAt,
            String failReason,
            String errorCode
    ) {
    }

    public record SettlementBatchRequest(
            Long marketId,
            String settlementId,
            List<SettlementItem> items
    ) {
        static SettlementBatchRequest from(MemberPointSettlementBatchRequest request) {
            return new SettlementBatchRequest(
                    request.marketId(),
                    request.settlementId(),
                    request.items().stream()
                            .map(SettlementItem::from)
                            .toList()
            );
        }
    }

    public record SettlementItem(
            Long predictionId,
            Long memberId,
            @JsonSerialize(using = BigDecimalPlainStringSerializer.class)
            BigDecimal amount,
            String referenceType,
            Long referenceId,
            String reason,
            String idempotencyKey
    ) {
        static SettlementItem from(MemberPointSettlementItem item) {
            return new SettlementItem(
                    item.predictionId(),
                    item.memberId(),
                    item.amount(),
                    item.referenceType(),
                    item.referenceId(),
                    item.reason(),
                    item.idempotencyKey()
            );
        }
    }

    public record SettlementBatchResponse(
            Long marketId,
            List<SettlementItemResult> results
    ) {
    }

    public record SettlementItemResult(
            Long predictionId,
            Long memberId,
            String status,
            String errorCode,
            BigDecimal amount,
            BigDecimal balanceSnapshot
    ) {
    }

    public record RefundBatchRequest(
            Long marketId,
            String refundId,
            List<RefundItem> items
    ) {
        static RefundBatchRequest from(MemberPointRefundBatchRequest request) {
            return new RefundBatchRequest(
                    request.marketId(),
                    request.refundId(),
                    request.items().stream()
                            .map(RefundItem::from)
                            .toList()
            );
        }
    }

    public record RefundItem(
            Long predictionId,
            Long memberId,
            @JsonSerialize(using = BigDecimalPlainStringSerializer.class)
            BigDecimal amount,
            String referenceType,
            Long referenceId,
            String reason,
            String idempotencyKey
    ) {
        static RefundItem from(MemberPointRefundItem item) {
            return new RefundItem(
                    item.predictionId(),
                    item.memberId(),
                    item.amount(),
                    item.referenceType(),
                    item.referenceId(),
                    item.reason(),
                    item.idempotencyKey()
            );
        }
    }

    public record RefundBatchResponse(
            Long marketId,
            List<RefundItemResult> results
    ) {
    }

    public record RefundItemResult(
            Long predictionId,
            Long memberId,
            String status,
            String errorCode,
            BigDecimal amount,
            BigDecimal balanceSnapshot
    ) {
    }
}
