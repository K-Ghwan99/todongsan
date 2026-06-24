package com.todongsan.battle_service.battle.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    private String description;
    private String optionA;
    private String optionB;
    private String sido;
    private String sigu;
    private String status;
    private boolean isClosed;
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
                .description(battle.getDescription())
                .optionA(battle.getOptionA())
                .optionB(battle.getOptionB())
                .sido(battle.getSido())
                .sigu(battle.getSigu())
                .status(battle.getStatus().name())
                .isClosed(battle.getStatus() == com.todongsan.battle_service.battle.entity.BattleStatus.CLOSED)
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

    // Lombok이 생성하는 isClosed()는 JSON 키가 "closed"가 되므로, 명시적으로 "isClosed"로 고정한다.
    @JsonProperty("isClosed")
    public boolean isClosed() {
        return isClosed;
    }
}
