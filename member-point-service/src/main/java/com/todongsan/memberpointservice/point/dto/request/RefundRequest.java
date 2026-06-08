package com.todongsan.memberpointservice.point.dto.request;

import lombok.Getter;

import java.util.List;

@Getter
public class RefundRequest {

    private Long marketId;    // Market 환불 시
    private Long referenceId; // Insight 환불 시
    private String refundId;
    private List<RefundItem> items;
}
