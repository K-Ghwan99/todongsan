package com.todongsan.battle_service.comment.service;

import com.todongsan.battle_service.battle.entity.Battle;
import com.todongsan.battle_service.battle.entity.BattleStatus;
import com.todongsan.battle_service.battle.repository.BattleRepository;
import com.todongsan.battle_service.client.MemberPointClient;
import com.todongsan.battle_service.client.dto.PointEarnRequest;
import com.todongsan.battle_service.comment.dto.request.CommentCreateRequest;
import com.todongsan.battle_service.comment.dto.response.CommentInternalResponse;
import com.todongsan.battle_service.comment.dto.response.CommentResponse;
import com.todongsan.battle_service.comment.entity.Comment;
import com.todongsan.battle_service.comment.repository.CommentRepository;
import com.todongsan.battle_service.global.exception.CustomException;
import com.todongsan.battle_service.global.exception.ErrorCode;
import com.todongsan.battle_service.retry.entity.PointRewardRetryQueue;
import com.todongsan.battle_service.retry.repository.PointRewardRetryQueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private static final int MAX_CONTENT_LENGTH = 500;
    private static final BigDecimal COMMENT_REWARD = BigDecimal.valueOf(2);

    private final BattleRepository battleRepository;
    private final CommentRepository commentRepository;
    private final MemberPointClient memberPointClient;
    private final PointRewardRetryQueueRepository retryQueueRepository;
    private final TransactionTemplate txTemplate;

    @Override
    public CommentResponse createComment(Long battleId, Long memberId, CommentCreateRequest request) {
        // 1) 검증 + 댓글 저장은 트랜잭션 안에서 (외부 REST 호출 제외)
        Comment comment = txTemplate.execute(status -> {
            findBattleForActivity(battleId);

            if (request.getContent().length() > MAX_CONTENT_LENGTH) {
                throw new CustomException(ErrorCode.BATTLE_COMMENT_TOO_LONG);
            }

            return commentRepository.save(Comment.builder()
                    .battleId(battleId)
                    .memberId(memberId)
                    .content(request.getContent())
                    .build());
        });

        // 2) 보상 지급은 트랜잭션 커밋 후 (외부 REST 호출은 트랜잭션 밖)
        earnCommentReward(battleId, memberId, comment.getId());

        return CommentResponse.from(comment);
    }

    private void earnCommentReward(Long battleId, Long memberId, Long commentId) {
        String idempotencyKey = "battle:comment:" + commentId + ":member:" + memberId;
        try {
            memberPointClient.earnPoint(PointEarnRequest.builder()
                    .memberId(memberId)
                    .type("EARN_COMMENT")
                    .referenceType("BATTLE")
                    .referenceId(battleId)
                    .amount(COMMENT_REWARD)
                    .idempotencyKey(idempotencyKey)
                    .build());
        } catch (CustomException e) {
            if (e.getErrorCode() == ErrorCode.EXTERNAL_SERVICE_TIMEOUT) {
                if (!retryQueueRepository.existsByIdempotencyKey(idempotencyKey)) {
                    retryQueueRepository.save(PointRewardRetryQueue.builder()
                            .memberId(memberId)
                            .referenceType("BATTLE")
                            .referenceId(battleId)
                            .type("EARN_COMMENT")
                            .amount(COMMENT_REWARD)
                            .idempotencyKey(idempotencyKey)
                            .build());
                }
                log.warn("Comment reward enqueued for retry: member={}, comment={}", memberId, commentId);
            } else {
                log.warn("Comment reward failed (4xx), manual correction needed: member={}, comment={}", memberId, commentId);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
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
