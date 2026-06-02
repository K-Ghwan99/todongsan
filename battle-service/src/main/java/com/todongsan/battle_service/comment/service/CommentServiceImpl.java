package com.todongsan.battle_service.comment.service;

import com.todongsan.battle_service.battle.entity.Battle;
import com.todongsan.battle_service.battle.entity.BattleStatus;
import com.todongsan.battle_service.battle.repository.BattleRepository;
import com.todongsan.battle_service.comment.dto.request.CommentCreateRequest;
import com.todongsan.battle_service.comment.dto.response.CommentInternalResponse;
import com.todongsan.battle_service.comment.dto.response.CommentResponse;
import com.todongsan.battle_service.comment.entity.Comment;
import com.todongsan.battle_service.comment.repository.CommentRepository;
import com.todongsan.battle_service.global.exception.CustomException;
import com.todongsan.battle_service.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private static final int MAX_CONTENT_LENGTH = 500;

    private final BattleRepository battleRepository;
    private final CommentRepository commentRepository;

    @Override
    @Transactional
    public CommentResponse createComment(Long battleId, Long memberId, CommentCreateRequest request) {
        Battle battle = findBattleForActivity(battleId);

        if (request.getContent().length() > MAX_CONTENT_LENGTH) {
            throw new CustomException(ErrorCode.BATTLE_COMMENT_TOO_LONG);
        }

        Comment comment = Comment.builder()
                .battleId(battleId)
                .memberId(memberId)
                .content(request.getContent())
                .build();
        commentRepository.save(comment);

        // TODO: Member-Point EARN_COMMENT 2P 지급 (Feature 5), 실패 시 RetryQueue 적재 (Feature 6)

        return CommentResponse.from(comment);
    }

    @Override
    public Page<CommentResponse> getComments(Long battleId, int page, int size) {
        battleRepository.findByIdAndDeletedAtIsNull(battleId)
                .orElseThrow(() -> new CustomException(ErrorCode.BATTLE_NOT_FOUND));

        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());
        return commentRepository.findByBattleIdAndDeletedAtIsNull(battleId, pageable)
                .map(CommentResponse::from);
    }

    @Override
    @Transactional
    public void deleteComment(Long battleId, Long commentId, Long memberId) {
        Comment comment = commentRepository.findByIdAndDeletedAtIsNull(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.BATTLE_COMMENT_NOT_FOUND));

        if (!comment.getMemberId().equals(memberId)) {
            throw new CustomException(ErrorCode.BATTLE_COMMENT_FORBIDDEN);
        }

        comment.softDelete();
    }

    @Override
    public CommentInternalResponse getCommentInternal(Long commentId) {
        Comment comment = commentRepository.findByIdAndDeletedAtIsNull(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.BATTLE_COMMENT_NOT_FOUND));
        return CommentInternalResponse.from(comment);
    }

    private Battle findBattleForActivity(Long battleId) {
        Battle battle = battleRepository.findByIdAndDeletedAtIsNull(battleId)
                .orElseThrow(() -> new CustomException(ErrorCode.BATTLE_NOT_FOUND));

        if (battle.getStatus() == BattleStatus.PENDING) {
            throw new CustomException(ErrorCode.BATTLE_NOT_FOUND);
        }
        if (battle.getStatus() == BattleStatus.CLOSED || battle.getStatus() == BattleStatus.CANCELLED) {
            throw new CustomException(ErrorCode.BATTLE_CLOSED);
        }
        return battle;
    }
}
