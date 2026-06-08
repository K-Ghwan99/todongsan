package com.todongsan.memberpointservice.point.dto.response;

import com.todongsan.memberpointservice.point.entity.PointHistory;
import lombok.Getter;

@Getter
public class EarnResponse {

    private final Long memberId;
    private final String type;
    private final String amount;
    private final String referenceType;
    private final Long referenceId;
    private final String balanceSnapshot;

    public EarnResponse(PointHistory history) {
        this.memberId = history.getMemberId();
        this.type = history.getType().name();
        this.amount = history.getAmount().toPlainString();
        this.referenceType = history.getReferenceType().name();
        this.referenceId = history.getReferenceId();
        this.balanceSnapshot = history.getBalanceSnapshot().toPlainString();
    }
}
