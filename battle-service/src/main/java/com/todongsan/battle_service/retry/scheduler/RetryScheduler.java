package com.todongsan.battle_service.retry.scheduler;

import com.todongsan.battle_service.client.MemberPointClient;
import com.todongsan.battle_service.client.dto.PointEarnRequest;
import com.todongsan.battle_service.retry.entity.PointRewardRetryQueue;
import com.todongsan.battle_service.retry.entity.RetryStatus;
import com.todongsan.battle_service.retry.repository.PointRewardRetryQueueRepository;
import com.todongsan.battle_service.vote.repository.BattleVoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RetryScheduler {

    private static final int MAX_RETRY = 3;
    private static final String TYPE_VOTE_WIN = "EARN_VOTE_WIN";

    private final PointRewardRetryQueueRepository retryQueueRepository;
    private final MemberPointClient memberPointClient;
    private final BattleVoteRepository battleVoteRepository;

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void retryPendingRewards() {
        List<PointRewardRetryQueue> pending =
                retryQueueRepository.findByStatusAndRetryCountLessThan(RetryStatus.PENDING, MAX_RETRY);

        for (PointRewardRetryQueue queue : pending) {
            try {
                PointEarnRequest request = PointEarnRequest.builder()
                        .memberId(queue.getMemberId())
                        .type(queue.getType())
                        .referenceType(queue.getReferenceType())
                        .referenceId(queue.getReferenceId())
                        .amount(queue.getAmount())
                        .reason(queue.getReason())
                        .idempotencyKey(queue.getIdempotencyKey())
                        .build();
                memberPointClient.earnPoint(request);
                queue.markSuccess();
                // 정산 승리 보상 재시도 성공 시 battle_vote.is_rewarded 동기화 (멱등성 플래그)
                if (TYPE_VOTE_WIN.equals(queue.getType())) {
                    battleVoteRepository.findByBattleIdAndMemberId(queue.getReferenceId(), queue.getMemberId())
                            .ifPresent(vote -> vote.markRewarded());
                }
                log.info("RetryQueue [{}] success", queue.getId());
            } catch (Exception e) {
                queue.incrementRetryCount();
                log.warn("RetryQueue [{}] failed (attempt {}): {}", queue.getId(), queue.getRetryCount(), e.getMessage());
            }
            retryQueueRepository.save(queue);
        }
    }
}
