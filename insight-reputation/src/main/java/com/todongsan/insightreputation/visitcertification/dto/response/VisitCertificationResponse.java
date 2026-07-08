package com.todongsan.insightreputation.visitcertification.dto.response;

import com.todongsan.insightreputation.enums.VisitCertMethod;
import com.todongsan.insightreputation.visitcertification.entity.VisitCertification;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class VisitCertificationResponse {
    
    private Long id;
    private String sido;
    private String sigu;
    private VisitCertMethod method;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String commentContent;
    private Long battleId;
    private LocalDateTime lastCertifiedAt;
    private LocalDateTime certifiedAt;
    private LocalDateTime nextAvailableDate;
    
    public static VisitCertificationResponse from(VisitCertification visitCertification) {
        return VisitCertificationResponse.builder()
                .id(visitCertification.getId())
                .sido(visitCertification.getSido())
                .sigu(visitCertification.getSigu())
                .method(visitCertification.getMethod())
                .latitude(visitCertification.getLatitude())
                .longitude(visitCertification.getLongitude())
                .commentContent(visitCertification.getCommentContent())
                .battleId(visitCertification.getBattleId())
                .lastCertifiedAt(visitCertification.getLastCertifiedAt())
                .certifiedAt(visitCertification.getCertifiedAt())
                .nextAvailableDate(visitCertification.getLastCertifiedAt().plusDays(30))
                .build();
    }
    
    public static List<VisitCertificationResponse> fromList(List<VisitCertification> visitCertifications) {
        return visitCertifications.stream()
                .map(VisitCertificationResponse::from)
                .collect(Collectors.toList());
    }
}