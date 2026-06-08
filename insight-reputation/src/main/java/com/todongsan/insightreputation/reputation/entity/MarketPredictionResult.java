package com.todongsan.insightreputation.reputation.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "market_prediction_result",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_member_market", 
            columnNames = {"member_id", "market_id"}
        )
    },
    indexes = {
        @Index(name = "idx_member_id", columnList = "member_id"),
        @Index(name = "idx_market_id", columnList = "market_id"),
        @Index(name = "idx_prediction_id", columnList = "prediction_id")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MarketPredictionResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "market_id", nullable = false)
    private Long marketId;

    @Column(name = "prediction_id")
    private Long predictionId;

    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public MarketPredictionResult(Long memberId, Long marketId, Long predictionId, Boolean isCorrect) {
        this.memberId = memberId;
        this.marketId = marketId;
        this.predictionId = predictionId;
        this.isCorrect = isCorrect;
        this.processedAt = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (processedAt == null) {
            processedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        processedAt = LocalDateTime.now();
    }
}