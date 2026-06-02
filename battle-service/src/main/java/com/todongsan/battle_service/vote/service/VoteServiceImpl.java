package com.todongsan.battle_service.vote.service;

import com.todongsan.battle_service.battle.entity.Battle;
import com.todongsan.battle_service.battle.entity.BattleStatus;
import com.todongsan.battle_service.battle.repository.BattleRepository;
import com.todongsan.battle_service.global.exception.CustomException;
import com.todongsan.battle_service.global.exception.ErrorCode;
import com.todongsan.battle_service.vote.dto.request.VoteRequest;
import com.todongsan.battle_service.vote.dto.response.*;
import com.todongsan.battle_service.vote.entity.BattleVote;
import com.todongsan.battle_service.vote.repository.BattleVoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VoteServiceImpl implements VoteService {

    private final BattleRepository battleRepository;
    private final BattleVoteRepository battleVoteRepository;

    private static final long RESULT_OPEN_HOURS = 72;

    @Override
    @Transactional
    public VoteResponse vote(Long battleId, Long memberId, VoteRequest request) {
        Battle battle = findActiveOrThrow(battleId);

        if (LocalDateTime.now().isBefore(battle.getStartAt())) {
            throw new CustomException(ErrorCode.BATTLE_CLOSED);
        }

        String option = request.getOption().toUpperCase();
        if (!option.equals("A") && !option.equals("B")) {
            throw new CustomException(ErrorCode.BATTLE_INVALID_OPTION);
        }

        // uq_battle_vote 충돌 → GlobalExceptionHandler에서 BATTLE_ALREADY_VOTED 변환
        BattleVote vote = BattleVote.builder()
                .battleId(battleId)
                .memberId(memberId)
                .selectedOption(option)
                .build();
        battleVoteRepository.save(vote);

        // 같은 트랜잭션에서 집계 UPDATE (원자성 보장)
        if (option.equals("A")) {
            battleRepository.incrementOptionA(battleId);
        } else {
            battleRepository.incrementOptionB(battleId);
        }

        // TODO: Member-Point EARN_VOTE 10P 지급 (Feature 5), 실패 시 RetryQueue 적재 (Feature 6)

        return VoteResponse.builder()
                .battleId(battleId)
                .selectedOption(option)
                .message("투표가 완료되었습니다.")
                .build();
    }

    @Override
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
    @Transactional
    public CrossAnalysisResponse getCrossResult(Long battleId, Long memberId, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new CustomException(ErrorCode.IDEMPOTENCY_KEY_REQUIRED);
        }

        Battle battle = battleRepository.findByIdAndDeletedAtIsNull(battleId)
                .orElseThrow(() -> new CustomException(ErrorCode.BATTLE_NOT_FOUND));

        if (battle.getStatus() != BattleStatus.CLOSED) {
            throw new CustomException(ErrorCode.BATTLE_RESULT_NOT_AVAILABLE);
        }

        // TODO: Member-Point SPEND_INSIGHT 30P 차감 (Feature 5)
        // TODO: 교차분석 데이터 조회 (Insight 연계 or 내부 집계)

        return CrossAnalysisResponse.builder()
                .battleId(battleId)
                .build();
    }

    @Override
    @Transactional
    public CertifiedResultResponse getCertifiedResult(Long battleId, Long memberId, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new CustomException(ErrorCode.IDEMPOTENCY_KEY_REQUIRED);
        }

        Battle battle = battleRepository.findByIdAndDeletedAtIsNull(battleId)
                .orElseThrow(() -> new CustomException(ErrorCode.BATTLE_NOT_FOUND));

        if (battle.getStatus() != BattleStatus.CLOSED) {
            throw new CustomException(ErrorCode.BATTLE_RESULT_NOT_AVAILABLE);
        }

        // TODO: Member-Point SPEND_INSIGHT 30P 차감 (Feature 5)
        // TODO: 방문 인증자 필터 데이터 조회

        return CertifiedResultResponse.builder()
                .battleId(battleId)
                .build();
    }

    @Override
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
