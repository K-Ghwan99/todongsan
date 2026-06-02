package com.todongsan.battle_service.battle.dto.response;

import com.todongsan.battle_service.battle.entity.Battle;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BattleStatusResponse {

    private Long battleId;
    private String status;

    public static BattleStatusResponse from(Battle battle) {
        return BattleStatusResponse.builder()
                .battleId(battle.getId())
                .status(battle.getStatus().name())
                .build();
    }
}
