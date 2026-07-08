package com.todongsan.battle_service.retry.entity;

import com.todongsan.battle_service.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "point_reward_retry_queue", indexes = {
        @Index(name = "idx_retry_status", columnList = "status, retry_count, created_at")
})
@Getter
@NoArgsConstructor
public class PointRewardRetryQueue extends BaseEntity {

    private static final int MAX_RETRY = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "reference_type", nullable = false, length = 50)
    private String referenceType; // 항상 "BATTLE"

    @Column(name = "reference_id", nullable = false)
    private Long referenceId; // battle.id

    @Column(nullable = false, length = 50)
    private String type; // PointHistoryType (EARN_VOTE, EARN_COMMENT 등)

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @Column(length = 100)
    private String reason;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RetryStatus status = RetryStatus.PENDING;

    @Builder
    public PointRewardRetryQueue(Long memberId, String referenceType, Long referenceId,
                                  String type, BigDecimal amount, String idempotencyKey, String reason) {
        this.memberId = memberId;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.type = type;
        this.amount = amount;
        this.idempotencyKey = idempotencyKey;
        this.reason = reason;
        this.retryCount = 0;
        this.status = RetryStatus.PENDING;
    }

    public void incrementRetryCount() {
        this.retryCount++;
        if (this.retryCount >= MAX_RETRY) {
            this.status = RetryStatus.FAILED;
        }
    }

    public void markSuccess() {
        this.status = RetryStatus.SUCCESS;
    }
}
