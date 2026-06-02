package com.todongsan.battle_service.battle.dto.response;

import com.todongsan.battle_service.battle.entity.Battle;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class BattleDetailResponse {

    private Long battleId;
    private String title;
    private String optionA;
    private String optionB;
    private String status;
    private int optionACount;
    private int optionBCount;
    private int voteCount;
    private String winningOption;
    private BigDecimal rewardAmount;
    private LocalDateTime settledAt;
    private Long createdBy;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private LocalDateTime createdAt;

    public static BattleDetailResponse from(Battle battle) {
        return BattleDetailResponse.builder()
                .battleId(battle.getId())
                .title(battle.getTitle())
                .optionA(battle.getOptionA())
                .optionB(battle.getOptionB())
                .status(battle.getStatus().name())
                .optionACount(battle.getOptionACount())
                .optionBCount(battle.getOptionBCount())
                .voteCount(battle.getVoteCount())
                .winningOption(battle.getWinningOption())
                .rewardAmount(battle.getRewardAmount())
                .settledAt(battle.getSettledAt())
                .createdBy(battle.getCreatedBy())
                .startAt(battle.getStartAt())
                .endAt(battle.getEndAt())
                .createdAt(battle.getCreatedAt())
                .build();
    }
}
