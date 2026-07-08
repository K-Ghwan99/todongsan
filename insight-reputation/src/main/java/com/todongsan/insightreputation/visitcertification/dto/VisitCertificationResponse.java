package com.todongsan.insightreputation.visitcertification.dto;

import com.todongsan.insightreputation.enums.VisitCertMethod;
import com.todongsan.insightreputation.visitcertification.entity.VisitCertification;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Schema(description = "방문 인증 등록 응답")
public class VisitCertificationResponse {

    @Schema(description = "시/도", example = "서울")
    private final String sido;

    @Schema(description = "시/구", example = "성동구")
    private final String sigu;

    @Schema(description = "인증 방법", example = "GPS")
    private final VisitCertMethod method;

    @Schema(description = "최초 인증 일시", example = "2024-11-15T10:30:00")
    private final LocalDateTime certifiedAt;

    @Schema(description = "최근 인증 일시", example = "2024-11-15T10:30:00")
    private final LocalDateTime lastCertifiedAt;

    @Schema(description = "다음 인증 가능 일시 (30일 후)", example = "2024-12-15T10:30:00")
    private final LocalDateTime nextAvailableDate;

    public VisitCertificationResponse(String sido, String sigu, VisitCertMethod method,
                                    LocalDateTime certifiedAt, LocalDateTime lastCertifiedAt,
                                    LocalDateTime nextAvailableDate) {
        this.sido = sido;
        this.sigu = sigu;
        this.method = method;
        this.certifiedAt = certifiedAt;
        this.lastCertifiedAt = lastCertifiedAt;
        this.nextAvailableDate = nextAvailableDate;
    }

    public static VisitCertificationResponse from(VisitCertification visitCertification) {
        LocalDateTime nextAvailableDate = visitCertification.getLastCertifiedAt().plusDays(30);
        
        return new VisitCertificationResponse(
            visitCertification.getSido(),
            visitCertification.getSigu(),
            visitCertification.getMethod(),
            visitCertification.getCertifiedAt(),
            visitCertification.getLastCertifiedAt(),
            nextAvailableDate
        );
    }
}