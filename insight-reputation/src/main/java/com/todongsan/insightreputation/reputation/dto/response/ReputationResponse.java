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
    private Integer activityCount;
    private LocalDateTime activityConfirmedAt;
    // Note: Sensitive fields like residenceSido, residenceSigu, predictionCorrect are excluded for other members
    
    public static ReputationResponse from(Reputation reputation) {
        return ReputationResponse.builder()
                .memberId(reputation.getMemberId())
                .activityScore(reputation.getActivityScore())
                .predictionCount(reputation.getPredictionCount())
                .predictionAccuracy(reputation.getPredictionAccuracy())
                .activityCount(reputation.getActivityCount())
                .activityConfirmedAt(reputation.getActivityConfirmedAt())
                .build();
    }
}