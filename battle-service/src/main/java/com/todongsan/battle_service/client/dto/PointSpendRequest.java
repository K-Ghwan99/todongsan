package com.todongsan.battle_service.client.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class PointSpendRequest {

    private Long memberId;
    private String type;           // PointHistoryType (예: SPEND_BATTLE_CREATE, SPEND_INSIGHT)
    private String referenceType;  // 항상 "BATTLE"
    private Long referenceId;      // battle.id
    private BigDecimal amount;
    private String idempotencyKey;
}
