package com.todongsan.battle_service.vote.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.todongsan.battle_service.battle.entity.Battle;
import com.todongsan.battle_service.vote.entity.BattleVote;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class MyVoteBattleResponse {

    private static final String DRAW = "DRAW";

    private Long battleId;
    private String title;
    private String optionA;
    private String optionB;
    private String sido;
    private String sigu;
    private String status;
    private String selectedOption;   // 내가 투표한 선택지 A/B
    private String winningOption;     // 정산 결과 A/B/DRAW, 미정산이면 null
    @JsonProperty("isWin")
    private Boolean isWin;            // 내 승리 여부, 미정산이면 null
    private String rewardAmount;      // 내가 받은 승리 보상(Decimal 문자열), 없으면 null
    private LocalDateTime settledAt;  // 정산 시각, null이면 미정산
    private LocalDateTime votedAt;    // 내가 투표한 시각
    private LocalDateTime startAt;
    private LocalDateTime endAt;

    public static MyVoteBattleResponse of(Battle battle, BattleVote vote) {
        boolean settled = battle.isSettled();
        String winningOption = battle.getWinningOption();

        Boolean isWin = null;
        String rewardAmount = null;
        if (settled) {
            isWin = winningOption != null
                    && !DRAW.equals(winningOption)
                    && winningOption.equals(vote.getSelectedOption());
            if (Boolean.TRUE.equals(isWin)
                    && battle.getRewardAmount() != null
                    && battle.getRewardAmount().compareTo(BigDecimal.ZERO) > 0) {
                rewardAmount = battle.getRewardAmount().toPlainString();
            }
        }

        return MyVoteBattleResponse.builder()
                .battleId(battle.getId())
                .title(battle.getTitle())
                .optionA(battle.getOptionA())
                .optionB(battle.getOptionB())
                .sido(battle.getSido())
                .sigu(battle.getSigu())
                .status(battle.getStatus().name())
                .selectedOption(vote.getSelectedOption())
                .winningOption(settled ? winningOption : null)
                .isWin(isWin)
                .rewardAmount(rewardAmount)
                .settledAt(battle.getSettledAt())
                .votedAt(vote.getCreatedAt())
                .startAt(battle.getStartAt())
                .endAt(battle.getEndAt())
                .build();
    }
}
