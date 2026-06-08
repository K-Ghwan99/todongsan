package com.todongsan.memberpointservice.point.dto.request;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class SettlementItem {

    private Long predictionId;
    private Long memberId;
    private BigDecimal amount;
    private String referenceType;
    private Long referenceId;
    private String reason;
    private String idempotencyKey;
}
