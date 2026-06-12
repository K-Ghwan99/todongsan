package com.todongsan.battle_service.vote.service;

import com.todongsan.battle_service.battle.entity.Battle;
import com.todongsan.battle_service.battle.entity.BattleStatus;
import com.todongsan.battle_service.battle.repository.BattleRepository;
import com.todongsan.battle_service.client.MemberPointClient;
import com.todongsan.battle_service.client.dto.PointEarnRequest;
import com.todongsan.battle_service.global.exception.CustomException;
import com.todongsan.battle_service.global.exception.ErrorCode;
import com.todongsan.battle_service.retry.entity.PointRewardRetryQueue;
import com.todongsan.battle_service.retry.repository.PointRewardRetryQueueRepository;
import com.todongsan.battle_service.vote.dto.request.VoteRequest;
import com.todongsan.battle_service.vote.dto.response.*;
import com.todongsan.battle_service.vote.entity.BattleVote;
import com.todongsan.battle_service.vote.repository.BattleVoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoteServiceImpl implements VoteService {

    private static final BigDecimal VOTE_REWARD = BigDecimal.valueOf(10);

    private final BattleRepository battleRepository;
    private final BattleVoteRepository battleVoteRepository;
    private final MemberPointClient memberPointClient;
    private final PointRewardRetryQueueRepository retryQueueRepository;
    private final TransactionTemplate txTemplate;

    private static final long RESULT_OPEN_HOURS = 72;

    @Override
    public VoteResponse vote(Long battleId, Long memberId, VoteRequest request) {
        // 1) 검증 + 투표 저장 + 집계는 한 트랜잭션 안에서 (외부 REST 호출 제외)
        String option = txTemplate.execute(status -> {
            Battle battle = findActiveOrThrow(battleId);

            if (LocalDateTime.now().isBefore(battle.getStartAt())) {
                throw new CustomException(ErrorCode.BATTLE_CLOSED);
            }

            String opt = request.getOption().toUpperCase();
            if (!opt.equals("A") && !opt.equals("B")) {
                throw new CustomException(ErrorCode.BATTLE_INVALID_OPTION);
            }

            // uq_battle_vote 충돌 → GlobalExceptionHandler에서 BATTLE_ALREADY_VOTED 변환
            battleVoteRepository.save(BattleVote.builder()
                    .battleId(battleId)
                    .memberId(memberId)
                    .selectedOption(opt)
                    .build());

            // 같은 트랜잭션에서 집계 UPDATE (원자성 보장)
            if (opt.equals("A")) {
                battleRepository.incrementOptionA(battleId);
            } else {
                battleRepository.incrementOptionB(battleId);
            }
            return opt;
        });

        // 2) 보상 지급은 트랜잭션 커밋 후 (외부 REST 호출은 트랜잭션 밖)
        earnVoteReward(battleId, memberId);

        return VoteResponse.builder()
                .battleId(battleId)
                .selectedOption(option)
                .message("투표가 완료되었습니다.")
                .build();
    }

    private void earnVoteReward(Long battleId, Long memberId) {
        String idempotencyKey = "battle:vote:" + battleId + ":member:" + memberId;
        try {
            memberPointClient.earnPoint(PointEarnRequest.builder()
                    .memberId(memberId)
                    .type("EARN_VOTE")
                    .referenceType("BATTLE")
                    .referenceId(battleId)
                    .amount(VOTE_REWARD)
                    .idempotencyKey(idempotencyKey)
                    .build());
        } catch (CustomException e) {
            if (e.getErrorCode() == ErrorCode.EXTERNAL_SERVICE_TIMEOUT) {
                if (!retryQueueRepository.existsByIdempotencyKey(idempotencyKey)) {
                    retryQueueRepository.save(PointRewardRetryQueue.builder()
                            .memberId(memberId)
                            .referenceType("BATTLE")
                            .referenceId(battleId)
                            .type("EARN_VOTE")
                            .amount(VOTE_REWARD)
                            .idempotencyKey(idempotencyKey)
                            .build());
                }
                log.warn("Vote reward enqueued for retry: member={}, battle={}", memberId, battleId);
            } else {
                log.warn("Vote reward failed (4xx), manual correction needed: member={}, battle={}", memberId, battleId);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public VoteResultResponse getResult(Long battleId, Long memberId) {
        Battle battle = battleRepository.findByIdAndDeletedAtIsNull(battleId)
                .orElseThrow(() -> new CustomException(ErrorCode.BATTLE_NOT_FOUND));

        boolean voted = memberId != null && battleVoteRepository.existsByBattleIdAndMemberId(battleId, memberId);
        boolean isActive = battle.getStatus() == BattleStatus.ACTIVE;
        boolean isClosed = battle.getStatus() == BattleStatus.CLOSED;
        boolean past72h = isClosed && battle.getEndAt().plusHours(RESULT_OPEN_HOURS).isBefore(LocalDateTime.now());

        // 결과 공개 정책 분기
        boolean resultVisible = voted || past72h;

        if (!resultVisible) {
            return VoteResultResponse.builder()
                    .battleId(battleId)
                    .status(battle.getStatus().name())
                    .voted(false)
                    .resultVisible(false)
                    .voteCount(battle.getVoteCount())
                    .message(isClosed ? "투표 종료 72시간 후 공개됩니다." : "투표 후 결과를 확인할 수 있습니다.")
                    .build();
        }

        int total = battle.getVoteCount();
        double aRatio = total > 0 ? (double) battle.getOptionACount() / total * 100 : 0;
        double bRatio = total > 0 ? (double) battle.getOptionBCount() / total * 100 : 0;

        return VoteResultResponse.builder()
                .battleId(battleId)
                .status(battle.getStatus().name())
                .voted(voted)
                .resultVisible(true)
                .optionACount(battle.getOptionACount())
                .optionBCount(battle.getOptionBCount())
                .voteCount(total)
                .optionARatio(aRatio)
                .optionBRatio(bRatio)
                .winningOption(battle.getWinningOption())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CrossAnalysisResponse getCrossResult(Long battleId) {
        Battle battle = battleRepository.findByIdAndDeletedAtIsNull(battleId)
                .orElseThrow(() -> new CustomException(ErrorCode.BATTLE_NOT_FOUND));

        if (battle.getStatus() != BattleStatus.CLOSED) {
            throw new CustomException(ErrorCode.BATTLE_RESULT_NOT_AVAILABLE);
        }

        // TODO: 교차분석 집계 데이터 조회 (Feature 3)

        return CrossAnalysisResponse.builder()
                .battleId(battleId)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CertifiedResultResponse getCertifiedResult(Long battleId) {
        Battle battle = battleRepository.findByIdAndDeletedAtIsNull(battleId)
                .orElseThrow(() -> new CustomException(ErrorCode.BATTLE_NOT_FOUND));

        if (battle.getStatus() != BattleStatus.CLOSED) {
            throw new CustomException(ErrorCode.BATTLE_RESULT_NOT_AVAILABLE);
        }

        // TODO: 방문 인증자 필터 집계 데이터 조회 (Feature 3)

        return CertifiedResultResponse.builder()
                .battleId(battleId)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public VoteRawResponse getRawVotes(Long battleId) {
        Battle battle = battleRepository.findByIdAndDeletedAtIsNull(battleId)
                .orElseThrow(() -> new CustomException(ErrorCode.BATTLE_NOT_FOUND));

        List<BattleVote> votes = battleVoteRepository.findByBattleId(battleId);
        return VoteRawResponse.from(battle, votes);
    }

    private Battle findActiveOrThrow(Long battleId) {
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
