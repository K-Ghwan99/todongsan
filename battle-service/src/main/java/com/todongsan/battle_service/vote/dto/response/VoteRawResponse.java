package com.todongsan.battle_service.vote.dto.response;

import com.todongsan.battle_service.battle.entity.Battle;
import com.todongsan.battle_service.vote.entity.BattleVote;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class VoteRawResponse {

    private Long battleId;
    private String title;
    private String optionA;
    private String optionB;
    private int totalVoteCount;
    private int optionACount;
    private int optionBCount;
    private String status;
    private String winningOption;
    private LocalDateTime settledAt;
    private List<VoteItem> votes;

    @Getter
    @Builder
    public static class VoteItem {
        private Long memberId;
        private String selectedOption;
    }

    public static VoteRawResponse from(Battle battle, List<BattleVote> votes) {
        List<VoteItem> voteItems = votes.stream()
                .map(v -> VoteItem.builder()
                        .memberId(v.getMemberId())
                        .selectedOption(v.getSelectedOption())
                        .build())
                .toList();

        return VoteRawResponse.builder()
                .battleId(battle.getId())
                .title(battle.getTitle())
                .optionA(battle.getOptionA())
                .optionB(battle.getOptionB())
                .totalVoteCount(battle.getVoteCount())
                .optionACount(battle.getOptionACount())
                .optionBCount(battle.getOptionBCount())
                .status(battle.getStatus().name())
                .winningOption(battle.getWinningOption())
                .settledAt(battle.getSettledAt())
                .votes(voteItems)
                .build();
    }
}
