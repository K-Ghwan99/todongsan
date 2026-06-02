package com.todongsan.battle_service.battle.controller;

import com.todongsan.battle_service.battle.dto.response.BattleDetailResponse;
import com.todongsan.battle_service.battle.service.BattleService;
import com.todongsan.battle_service.comment.dto.response.CommentInternalResponse;
import com.todongsan.battle_service.comment.service.CommentService;
import com.todongsan.battle_service.global.exception.CustomException;
import com.todongsan.battle_service.global.exception.ErrorCode;
import com.todongsan.battle_service.global.response.ApiResponse;
import com.todongsan.battle_service.vote.dto.response.VoteRawResponse;
import com.todongsan.battle_service.vote.service.VoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/battles")
@RequiredArgsConstructor
public class BattleInternalController {

    private final BattleService battleService;
    private final VoteService voteService;
    private final CommentService commentService;

    @Value("${external.internal-auth-token}")
    private String internalAuthToken;

    // GET /api/v1/battles/{battleId}/votes/raw (Insight 전용)
    @GetMapping("/{battleId}/votes/raw")
    public ApiResponse<VoteRawResponse> getRawVotes(
            @PathVariable Long battleId,
            @RequestHeader("X-Internal-Auth") String authToken) {
        validateInternalAuth(authToken);
        return ApiResponse.ok(voteService.getRawVotes(battleId));
    }

    // GET /api/v1/battles/comments/{commentId} (Insight 방문 인증 전용)
    @GetMapping("/comments/{commentId}")
    public ApiResponse<CommentInternalResponse> getComment(
            @PathVariable Long commentId,
            @RequestHeader("X-Internal-Auth") String authToken) {
        validateInternalAuth(authToken);
        return ApiResponse.ok(commentService.getCommentInternal(commentId));
    }

    // GET /api/v1/battles/{battleId}/info (Insight AI 분석 전용)
    @GetMapping("/{battleId}/info")
    public ApiResponse<BattleDetailResponse> getBattleInfo(
            @PathVariable Long battleId,
            @RequestHeader("X-Internal-Auth") String authToken) {
        validateInternalAuth(authToken);
        return ApiResponse.ok(battleService.getBattleInternal(battleId));
    }

    private void validateInternalAuth(String token) {
        if (!internalAuthToken.equals(token)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }
}
