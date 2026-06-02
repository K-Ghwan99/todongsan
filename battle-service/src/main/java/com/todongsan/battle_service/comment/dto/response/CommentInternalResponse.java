package com.todongsan.battle_service.comment.dto.response;

import com.todongsan.battle_service.comment.entity.Comment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CommentInternalResponse {

    private Long commentId;
    private Long battleId;
    private Long memberId;
    private LocalDateTime createdAt;

    public static CommentInternalResponse from(Comment comment) {
        return CommentInternalResponse.builder()
                .commentId(comment.getId())
                .battleId(comment.getBattleId())
                .memberId(comment.getMemberId())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
