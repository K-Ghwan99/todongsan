package com.todongsan.battle_service.retry.repository;

import com.todongsan.battle_service.retry.entity.PointRewardRetryQueue;
import com.todongsan.battle_service.retry.entity.RetryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PointRewardRetryQueueRepository extends JpaRepository<PointRewardRetryQueue, Long> {

    // 재시도 배치: PENDING이고 최대 3회 미만인 것만 조회
    List<PointRewardRetryQueue> findByStatusAndRetryCountLessThan(RetryStatus status, int maxRetry);

    boolean existsByIdempotencyKey(String idempotencyKey);
}
