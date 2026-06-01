package com.todongsan.insightreputation.reputation.dto.response;

import com.todongsan.insightreputation.reputation.entity.Reputation;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class ReputationResponse {
    
    private Long memberId;
    private Integer activityScore;
    private Integer predictionCount;
    private BigDecimal predictionAccuracy;
    private String residenceSido;
    private String residenceSigu;
    private LocalDateTime residenceDeclaredAt;
    private LocalDateTime residenceChangedAt;
    private Integer activityCount;
    private LocalDateTime activityConfirmedAt;
    
    public static ReputationResponse from(Reputation reputation) {
        return ReputationResponse.builder()
                .memberId(reputation.getMemberId())
                .activityScore(reputation.getActivityScore())
                .predictionCount(reputation.getPredictionCount())
                .predictionAccuracy(reputation.getPredictionAccuracy())
                .residenceSido(reputation.getResidenceSido())
                .residenceSigu(reputation.getResidenceSigu())
                .residenceDeclaredAt(reputation.getResidenceDeclaredAt())
                .residenceChangedAt(reputation.getResidenceChangedAt())
                .activityCount(reputation.getActivityCount())
                .activityConfirmedAt(reputation.getActivityConfirmedAt())
                .build();
    }
}