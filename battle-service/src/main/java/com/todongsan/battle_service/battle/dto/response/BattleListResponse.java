package com.todongsan.battle_service.battle.dto.response;

import com.todongsan.battle_service.battle.entity.Battle;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class BattleListResponse {

    private Long battleId;
    private String title;
    private String optionA;
    private String optionB;
    private String status;
    private int voteCount;
    private int optionACount;
    private int optionBCount;
    private long commentCount;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private LocalDateTime createdAt;

    public static BattleListResponse from(Battle battle, long commentCount) {
        return BattleListResponse.builder()
                .battleId(battle.getId())
                .title(battle.getTitle())
                .optionA(battle.getOptionA())
                .optionB(battle.getOptionB())
                .status(battle.getStatus().name())
                .voteCount(battle.getVoteCount())
                .optionACount(battle.getOptionACount())
                .optionBCount(battle.getOptionBCount())
                .commentCount(commentCount)
                .startAt(battle.getStartAt())
                .endAt(battle.getEndAt())
                .createdAt(battle.getCreatedAt())
                .build();
    }
}
