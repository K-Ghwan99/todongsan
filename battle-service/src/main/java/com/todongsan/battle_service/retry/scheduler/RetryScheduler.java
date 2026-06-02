package com.todongsan.battle_service.retry.scheduler;

import com.todongsan.battle_service.client.MemberPointClient;
import com.todongsan.battle_service.client.dto.PointEarnRequest;
import com.todongsan.battle_service.retry.entity.PointRewardRetryQueue;
import com.todongsan.battle_service.retry.entity.RetryStatus;
import com.todongsan.battle_service.retry.repository.PointRewardRetryQueueRepository;
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

    private final PointRewardRetryQueueRepository retryQueueRepository;
    private final MemberPointClient memberPointClient;

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
                        .idempotencyKey(queue.getIdempotencyKey())
                        .build();
                memberPointClient.earnPoint(request);
                queue.markSuccess();
                log.info("RetryQueue [{}] success", queue.getId());
            } catch (Exception e) {
                queue.incrementRetryCount();
                log.warn("RetryQueue [{}] failed (attempt {}): {}", queue.getId(), queue.getRetryCount(), e.getMessage());
            }
            retryQueueRepository.save(queue);
        }
    }
}
