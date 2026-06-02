package com.todongsan.battle_service.battle.dto.response;

import com.todongsan.battle_service.battle.entity.Battle;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class BattleCreateResponse {

    private Long battleId;
    private String title;
    private String optionA;
    private String optionB;
    private String status;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private LocalDateTime createdAt;

    public static BattleCreateResponse from(Battle battle) {
        return BattleCreateResponse.builder()
                .battleId(battle.getId())
                .title(battle.getTitle())
                .optionA(battle.getOptionA())
                .optionB(battle.getOptionB())
                .status(battle.getStatus().name())
                .startAt(battle.getStartAt())
                .endAt(battle.getEndAt())
                .createdAt(battle.getCreatedAt())
                .build();
    }
}
