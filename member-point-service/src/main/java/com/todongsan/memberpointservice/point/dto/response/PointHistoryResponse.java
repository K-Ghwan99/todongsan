package com.todongsan.memberpointservice.point.dto.response;

import com.todongsan.memberpointservice.point.entity.PointHistory;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class PointHistoryResponse {

    private final Long id;
    private final String type;
    private final String amount;
    private final String balanceSnapshot;
    private final String reason;
    private final Long referenceId;
    private final LocalDateTime createdAt;

    public PointHistoryResponse(PointHistory pointHistory) {
        this.id = pointHistory.getId();
        this.type = pointHistory.getType().name();
        this.amount = pointHistory.getAmount().toPlainString();
        this.balanceSnapshot = pointHistory.getBalanceSnapshot().toPlainString();
        this.reason = pointHistory.getReason();
        this.referenceId = pointHistory.getReferenceId();
        this.createdAt = pointHistory.getCreatedAt();
    }

}
