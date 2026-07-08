package com.todongsan.insightreputation.visitcertification.dto.response;

import com.todongsan.insightreputation.enums.VisitCertMethod;
import com.todongsan.insightreputation.visitcertification.entity.VisitCertification;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class VisitCertificationSummary {
    
    private String sido;
    private String sigu;
    private VisitCertMethod method;
    private LocalDateTime certifiedAt;
    private LocalDateTime lastCertifiedAt;
    private LocalDateTime nextAvailableDate;
    
    public static VisitCertificationSummary from(VisitCertification certification) {
        return VisitCertificationSummary.builder()
                .sido(certification.getSido())
                .sigu(certification.getSigu())
                .method(certification.getMethod())
                .certifiedAt(certification.getCertifiedAt())
                .lastCertifiedAt(certification.getLastCertifiedAt())
                .nextAvailableDate(certification.getLastCertifiedAt().plusDays(30))
                .build();
    }
    
    public static List<VisitCertificationSummary> fromList(List<VisitCertification> certifications) {
        return certifications.stream()
                .map(VisitCertificationSummary::from)
                .collect(Collectors.toList());
    }
}