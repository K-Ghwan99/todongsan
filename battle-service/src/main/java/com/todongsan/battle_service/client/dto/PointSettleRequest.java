package com.todongsan.battle_service.client.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class PointSettleRequest {

    private Long memberId;
    private String type;           // 항상 "EARN_VOTE_WIN"
    private String referenceType;  // 항상 "BATTLE"
    private Long referenceId;      // battle.id
    private BigDecimal amount;
    private String idempotencyKey; // "battle:settle:{battleId}:member:{memberId}"
}
