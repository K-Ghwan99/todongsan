package com.todongsan.battle_service.battle.scheduler;

import com.todongsan.battle_service.battle.entity.Battle;
import com.todongsan.battle_service.battle.repository.BattleRepository;
import com.todongsan.battle_service.client.MemberPointClient;
import com.todongsan.battle_service.client.dto.PointEarnRequest;
import com.todongsan.battle_service.retry.entity.PointRewardRetryQueue;
import com.todongsan.battle_service.retry.repository.PointRewardRetryQueueRepository;
import com.todongsan.battle_service.vote.entity.BattleVote;
import com.todongsan.battle_service.vote.repository.BattleVoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BattleSettleScheduler {

    private static final BigDecimal VOTE_WIN_REWARD = BigDecimal.valueOf(10);
    private static final String DRAW = "DRAW";

    private final BattleRepository battleRepository;
    private final BattleVoteRepository battleVoteRepository;
    private final MemberPointClient memberPointClient;
    private final PointRewardRetryQueueRepository retryQueueRepository;
    private final TransactionTemplate txTemplate;

    @Scheduled(fixedDelay = 60000)
    public void settleClosedBattles() {
        List<Long> battleIds = txTemplate.execute(status ->
                battleRepository.findUnsettledClosedBattles().stream()
                        .map(Battle::getId)
                        .toList());

        if (battleIds == null) return;
        for (Long battleId : battleIds) {
            try {
                settleOneBattle(battleId);
            } catch (Exception e) {
                log.error("Battle [{}] 정산 실패", battleId, e);
            }
        }
    }

    /**
     * Battle 한 건 정산. 외부 REST 호출(보상 지급)은 트랜잭션 밖에서 수행한다.
     * 1) (tx) 승자 진영 미보상 투표자 조회. DRAW면 보상 없이 즉시 정산 완료.
     * 2) (no tx) 투표자별 보상 REST 호출 → 성공/실패 분리
     * 3) (tx) 성공자 is_rewarded=true, 실패자 RetryQueue 적재, Battle settled 처리
     */
    private void settleOneBattle(Long battleId) {
        List<Long> winnerMemberIds = txTemplate.execute(status -> {
            Battle battle = battleRepository.findById(battleId).orElse(null);
            if (battle == null || battle.isSettled()) {
                return null;
            }
            if (DRAW.equals(battle.getWinningOption())) {
                battle.settle(BigDecimal.ZERO);
                log.info("Battle [{}] settled as DRAW", battleId);
                return null;
            }
            return battleVoteRepository
                    .findByBattleIdAndSelectedOptionAndIsRewardedFalse(battleId, battle.getWinningOption())
                    .stream()
                    .map(BattleVote::getMemberId)
                    .toList();
        });

        if (winnerMemberIds == null) {
            return; // DRAW 또는 이미 정산됨
        }

        List<Long> rewarded = new ArrayList<>();
        List<Long> failed = new ArrayList<>();
        for (Long memberId : winnerMemberIds) {
            try {
                memberPointClient.earnPoint(PointEarnRequest.builder()
                        .memberId(memberId)
                        .type("EARN_VOTE_WIN")
                        .referenceType("BATTLE")
                        .referenceId(battleId)
                        .amount(VOTE_WIN_REWARD)
                        .reason("배틀 승리 보상")
                        .idempotencyKey(settleKey(battleId, memberId))
                        .build());
                rewarded.add(memberId);
            } catch (Exception e) {
                log.warn("Battle [{}] member [{}] 승리 보상 실패, 재시도 적재", battleId, memberId);
                failed.add(memberId);
            }
        }

        txTemplate.executeWithoutResult(status -> {
            for (Long memberId : rewarded) {
                battleVoteRepository.findByBattleIdAndMemberId(battleId, memberId)
                        .ifPresent(BattleVote::markRewarded);
            }
            for (Long memberId : failed) {
                String key = settleKey(battleId, memberId);
                if (!retryQueueRepository.existsByIdempotencyKey(key)) {
                    retryQueueRepository.save(PointRewardRetryQueue.builder()
                            .memberId(memberId)
                            .referenceType("BATTLE")
                            .referenceId(battleId)
                            .type("EARN_VOTE_WIN")
                            .amount(VOTE_WIN_REWARD)
                            .reason("배틀 승리 보상")
                            .idempotencyKey(key)
                            .build());
                }
            }
            battleRepository.findById(battleId).ifPresent(b -> b.settle(VOTE_WIN_REWARD));
            log.info("Battle [{}] settled. rewarded={}, retryEnqueued={}", battleId, rewarded.size(), failed.size());
        });
    }

    private String settleKey(Long battleId, Long memberId) {
        return "battle:settle:" + battleId + ":member:" + memberId;
    }
}
