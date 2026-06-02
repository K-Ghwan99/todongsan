package com.todongsan.battle_service.comment.service;

import com.todongsan.battle_service.comment.dto.request.CommentCreateRequest;
import com.todongsan.battle_service.comment.dto.response.CommentInternalResponse;
import com.todongsan.battle_service.comment.dto.response.CommentResponse;
import org.springframework.data.domain.Page;

public interface CommentService {

    CommentResponse createComment(Long battleId, Long memberId, CommentCreateRequest request);

    Page<CommentResponse> getComments(Long battleId, int page, int size);

    void deleteComment(Long battleId, Long commentId, Long memberId);

    CommentInternalResponse getCommentInternal(Long commentId);
}
