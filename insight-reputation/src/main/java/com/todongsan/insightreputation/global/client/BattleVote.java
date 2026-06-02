package com.todongsan.insightreputation.global.client;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class BattleVote {
    
    private Long memberId;
    private String selectedOption;  // "A" 또는 "B"
    private LocalDateTime votedAt;
}