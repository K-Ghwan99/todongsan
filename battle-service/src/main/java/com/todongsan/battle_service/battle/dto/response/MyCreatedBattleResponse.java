package com.todongsan.battle_service.battle.dto.response;

import com.todongsan.battle_service.battle.entity.Battle;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MyCreatedBattleResponse {

    private Long battleId;
    private String title;
    private String optionA;
    private String optionB;
    private String sido;
    private String sigu;
    private String status;
    private int voteCount;
    private String winningOption;     // 정산 결과 A/B/DRAW, 미정산이면 null
    private LocalDateTime settledAt;  // 정산 시각, null이면 미정산
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private LocalDateTime createdAt;

    public static MyCreatedBattleResponse from(Battle battle) {
        boolean settled = battle.isSettled();
        return MyCreatedBattleResponse.builder()
                .battleId(battle.getId())
                .title(battle.getTitle())
                .optionA(battle.getOptionA())
                .optionB(battle.getOptionB())
                .sido(battle.getSido())
                .sigu(battle.getSigu())
                .status(battle.getStatus().name())
                .voteCount(battle.getVoteCount())
                .winningOption(settled ? battle.getWinningOption() : null)
                .settledAt(battle.getSettledAt())
                .startAt(battle.getStartAt())
                .endAt(battle.getEndAt())
                .createdAt(battle.getCreatedAt())
                .build();
    }
}
