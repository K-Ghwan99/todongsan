package com.todongsan.battle_service.battle.scheduler;

import com.todongsan.battle_service.battle.entity.Battle;
import com.todongsan.battle_service.battle.repository.BattleRepository;
import com.todongsan.battle_service.client.MemberPointClient;
import com.todongsan.battle_service.client.dto.PointSettleRequest;
import com.todongsan.battle_service.retry.entity.PointRewardRetryQueue;
import com.todongsan.battle_service.retry.entity.RetryStatus;
import com.todongsan.battle_service.retry.repository.PointRewardRetryQueueRepository;
import com.todongsan.battle_service.vote.entity.BattleVote;
import com.todongsan.battle_service.vote.repository.BattleVoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BattleSettleScheduler {

    private static final BigDecimal VOTE_WIN_REWARD = BigDecimal.valueOf(10);

    private final BattleRepository battleRepository;
    private final BattleVoteRepository battleVoteRepository;
    private final MemberPointClient memberPointClient;
    private final PointRewardRetryQueueRepository retryQueueRepository;

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void settleClosedBattles() {
        List<Battle> unsettled = battleRepository.findUnsettledClosedBattles();

        for (Battle battle : unsettled) {
            if ("DRAW".equals(battle.getWinningOption())) {
                battle.settle(BigDecimal.ZERO);
                log.info("Battle [{}] settled as DRAW", battle.getId());
                continue;
            }

            List<BattleVote> winners = battleVoteRepository
                    .findByBattleIdAndSelectedOptionAndIsRewardedFalse(battle.getId(), battle.getWinningOption());

            for (BattleVote vote : winners) {
                String idempotencyKey = "battle:settle:" + battle.getId() + ":member:" + vote.getMemberId();
                try {
                    PointSettleRequest req = PointSettleRequest.builder()
                            .memberId(vote.getMemberId())
                            .type("EARN_VOTE_WIN")
                            .referenceType("BATTLE")
                            .referenceId(battle.getId())
                            .amount(VOTE_WIN_REWARD)
                            .idempotencyKey(idempotencyKey)
                            .build();
                    memberPointClient.settlePoints(List.of(req));
                    vote.markRewarded();
                } catch (Exception e) {
                    log.warn("Settle reward failed for member [{}], enqueue retry", vote.getMemberId());
                    if (!retryQueueRepository.existsByIdempotencyKey(idempotencyKey)) {
                        retryQueueRepository.save(PointRewardRetryQueue.builder()
                                .memberId(vote.getMemberId())
                                .referenceType("BATTLE")
                                .referenceId(battle.getId())
                                .type("EARN_VOTE_WIN")
                                .amount(VOTE_WIN_REWARD)
                                .idempotencyKey(idempotencyKey)
                                .build());
                    }
                }
            }

            battle.settle(VOTE_WIN_REWARD);
            log.info("Battle [{}] settled. winner={}", battle.getId(), battle.getWinningOption());
        }
    }
}
