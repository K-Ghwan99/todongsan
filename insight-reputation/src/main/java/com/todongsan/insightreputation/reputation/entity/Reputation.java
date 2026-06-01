package com.todongsan.insightreputation.reputation.entity;

import com.todongsan.insightreputation.global.config.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "reputation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reputation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false, unique = true)
    private Long memberId;

    @Column(name = "activity_score", nullable = false)
    private Integer activityScore = 0;

    @Column(name = "prediction_count", nullable = false)
    private Integer predictionCount = 0;

    @Column(name = "prediction_correct", nullable = false)
    private Integer predictionCorrect = 0;

    @Column(name = "prediction_accuracy", nullable = false, precision = 5, scale = 2)
    private BigDecimal predictionAccuracy = BigDecimal.ZERO;

    @Column(name = "residence_sido", length = 50)
    private String residenceSido;

    @Column(name = "residence_sigu", length = 50)
    private String residenceSigu;

    @Column(name = "residence_declared_at")
    private LocalDateTime residenceDeclaredAt;

    @Column(name = "residence_changed_at")
    private LocalDateTime residenceChangedAt;

    @Column(name = "activity_count", nullable = false)
    private Integer activityCount = 0;

    @Column(name = "activity_confirmed_at")
    private LocalDateTime activityConfirmedAt;

    @Builder
    public Reputation(Long memberId, String residenceSido, String residenceSigu) {
        this.memberId = memberId;
        this.residenceSido = residenceSido;
        this.residenceSigu = residenceSigu;
        this.residenceDeclaredAt = LocalDateTime.now();
        this.residenceChangedAt = LocalDateTime.now();
        this.activityScore = 0;
        this.predictionCount = 0;
        this.predictionCorrect = 0;
        this.predictionAccuracy = BigDecimal.ZERO;
        this.activityCount = 0;
    }

    public void updateActivityScore(Integer score) {
        this.activityScore = score;
    }

    public void updatePredictionStats(Integer count, Integer correct) {
        this.predictionCount = count;
        this.predictionCorrect = correct;
        
        if (count > 0) {
            // CLAUDE.md 요구사항: FLOOR(correct/count * 100 * 100) / 100 (버림)
            BigDecimal accuracy = new BigDecimal(correct)
                    .multiply(new BigDecimal("100"))
                    .multiply(new BigDecimal("100"))
                    .divide(new BigDecimal(count), 0, BigDecimal.ROUND_DOWN)
                    .divide(new BigDecimal("100"), 2, BigDecimal.ROUND_DOWN);
            this.predictionAccuracy = accuracy;
        } else {
            this.predictionAccuracy = BigDecimal.ZERO;
        }
    }

    public void changeResidence(String sido, String sigu) {
        this.residenceSido = sido;
        this.residenceSigu = sigu;
        this.residenceChangedAt = LocalDateTime.now();
        this.activityCount = 0;
        this.activityConfirmedAt = null;
    }

    public void incrementActivityCount() {
        if (this.activityConfirmedAt == null && this.activityCount < 3) {
            this.activityCount++;
            if (this.activityCount >= 3) {
                this.activityConfirmedAt = LocalDateTime.now();
            }
        }
    }
}