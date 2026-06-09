package com.todongsan.memberpointservice.point.dto.request;

import lombok.Getter;

import java.util.List;

@Getter
public class SettlementRequest {

    private Long marketId;
    private String settlementId;
    private List<SettlementItem> items;
}
