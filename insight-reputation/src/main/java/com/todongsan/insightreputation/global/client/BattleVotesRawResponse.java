package com.todongsan.insightreputation.global.client;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class BattleVotesRawResponse {
    
    private Long battleId;
    private List<BattleVote> votes;
    private Integer totalVotes;
}