package com.todongsan.insightreputation.global.client;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class BattleCommentResponse {
    
    private Long commentId;
    private Long battleId;
    private Long memberId;
    private LocalDateTime createdAt;
}