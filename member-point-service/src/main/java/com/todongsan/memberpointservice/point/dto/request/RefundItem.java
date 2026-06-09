package com.todongsan.memberpointservice.point.dto.request;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class RefundItem {

    private Long predictionId;  // Market 환불만 존재, Insight는 null
    private Long memberId;
    private BigDecimal amount;
    private String referenceType;
    private Long referenceId;
    private String reason;
    private String idempotencyKey;
}
