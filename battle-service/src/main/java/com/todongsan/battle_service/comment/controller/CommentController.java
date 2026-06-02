package com.todongsan.battle_service.comment.controller;

import com.todongsan.battle_service.comment.dto.request.CommentCreateRequest;
import com.todongsan.battle_service.comment.dto.response.CommentResponse;
import com.todongsan.battle_service.comment.service.CommentService;
import com.todongsan.battle_service.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/battles")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // POST /api/v1/battles/{battleId}/comments
    @PostMapping("/{battleId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CommentResponse> createComment(
            @PathVariable Long battleId,
            @RequestHeader("X-Member-Id") Long memberId,
            @Valid @RequestBody CommentCreateRequest request) {
        return ApiResponse.ok(commentService.createComment(battleId, memberId, request));
    }

    // GET /api/v1/battles/{battleId}/comments?page=0&size=10
    @GetMapping("/{battleId}/comments")
    public ApiResponse<Page<CommentResponse>> getComments(
            @PathVariable Long battleId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(commentService.getComments(battleId, page, size));
    }

    // DELETE /api/v1/battles/{battleId}/comments/{commentId}
    @DeleteMapping("/{battleId}/comments/{commentId}")
    public ApiResponse<Void> deleteComment(
            @PathVariable Long battleId,
            @PathVariable Long commentId,
            @RequestHeader("X-Member-Id") Long memberId) {
        commentService.deleteComment(battleId, commentId, memberId);
        return ApiResponse.ok();
    }
}
