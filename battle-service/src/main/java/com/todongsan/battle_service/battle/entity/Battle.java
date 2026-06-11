package com.todongsan.battle_service.battle.entity;

import com.todongsan.battle_service.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "battle", indexes = {
        @Index(name = "idx_battle_status_end", columnList = "status, end_at, deleted_at")
})
@Getter
@NoArgsConstructor
public class Battle extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(name = "option_a", nullable = false, length = 100)
    private String optionA;

    @Column(name = "option_b", nullable = false, length = 100)
    private String optionB;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BattleStatus status;

    @Column(name = "option_a_count", nullable = false)
    private int optionACount = 0;

    @Column(name = "option_b_count", nullable = false)
    private int optionBCount = 0;

    @Column(name = "vote_count", nullable = false)
    private int voteCount = 0;

    @Column(name = "winning_option", length = 4)
    private String winningOption; // 'A', 'B', 'DRAW' (정산 전 NULL)

    @Column(name = "reward_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal rewardAmount = BigDecimal.ZERO;

    @Column(name = "settled_at")
    private LocalDateTime settledAt; // NULL이면 미정산

    @Column(name = "sido", length = 50)
    private String sido;

    @Column(name = "sigu", length = 50)
    private String sigu;

    @Column(name = "created_by", nullable = false)
    private Long createdBy; // member.id (REST 참조)

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public Battle(String title, String optionA, String optionB, String sido, String sigu,
                  Long createdBy, LocalDateTime startAt, LocalDateTime endAt) {
        this.title = title;
        this.optionA = optionA;
        this.optionB = optionB;
        this.sido = sido;
        this.sigu = sigu;
        this.status = BattleStatus.PENDING;
        this.createdBy = createdBy;
        this.startAt = startAt;
        this.endAt = endAt;
        this.rewardAmount = BigDecimal.ZERO;
        this.optionACount = 0;
        this.optionBCount = 0;
        this.voteCount = 0;
    }

    public void approve() {
        this.status = BattleStatus.ACTIVE;
    }

    public void reject() {
        this.status = BattleStatus.CANCELLED;
    }

    public void cancel() {
        this.status = BattleStatus.CANCELLED;
    }

    public void close(String winningOption) {
        this.status = BattleStatus.CLOSED;
        this.winningOption = winningOption;
    }

    public void settle(BigDecimal rewardAmount) {
        this.rewardAmount = rewardAmount;
        this.settledAt = LocalDateTime.now();
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean isSettled() {
        return settledAt != null;
    }
}
