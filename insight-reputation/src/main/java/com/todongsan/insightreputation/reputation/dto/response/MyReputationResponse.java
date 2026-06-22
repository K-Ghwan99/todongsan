package com.todongsan.insightreputation.reputation.dto.response;

import com.todongsan.insightreputation.reputation.entity.Reputation;
import com.todongsan.insightreputation.visitcertification.dto.response.VisitCertificationSummary;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class MyReputationResponse {
    
    private Long memberId;
    private Integer activityScore;
    private Integer predictionCount;
    private Integer predictionCorrect;
    private BigDecimal predictionAccuracy;
    private String residenceSido;
    private String residenceSigu;
    private LocalDateTime residenceDeclaredAt;
    private LocalDateTime residenceChangedAt;
    private Integer activityCount;
    private LocalDateTime activityConfirmedAt;
    private List<VisitCertificationSummary> visitCertifications;
    private Integer visitCertificationCount;

    public static MyReputationResponse from(Reputation reputation, List<VisitCertificationSummary> visitCertifications) {
        return MyReputationResponse.builder()
                .memberId(reputation.getMemberId())
                .activityScore(reputation.getActivityScore())
                .predictionCount(reputation.getPredictionCount())
                .predictionCorrect(reputation.getPredictionCorrect())
                .predictionAccuracy(reputation.getPredictionAccuracy())
                .residenceSido(reputation.getResidenceSido())
                .residenceSigu(reputation.getResidenceSigu())
                .residenceDeclaredAt(reputation.getResidenceDeclaredAt())
                .residenceChangedAt(reputation.getResidenceChangedAt())
                .activityCount(reputation.getActivityCount())
                .activityConfirmedAt(reputation.getActivityConfirmedAt())
                .visitCertifications(visitCertifications)
                .visitCertificationCount(visitCertifications != null ? visitCertifications.size() : 0)
                .build();
    }
}