package com.todongsan.insightreputation.visitcertification.dto;

import com.todongsan.insightreputation.enums.VisitCertMethod;
import com.todongsan.insightreputation.visitcertification.entity.VisitCertification;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Schema(description = "방문 인증 목록 항목")
public class VisitCertificationListResponse {

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

    public VisitCertificationListResponse(String sido, String sigu, VisitCertMethod method,
                                        LocalDateTime certifiedAt, LocalDateTime lastCertifiedAt) {
        this.sido = sido;
        this.sigu = sigu;
        this.method = method;
        this.certifiedAt = certifiedAt;
        this.lastCertifiedAt = lastCertifiedAt;
    }

    public static VisitCertificationListResponse from(VisitCertification visitCertification) {
        return new VisitCertificationListResponse(
            visitCertification.getSido(),
            visitCertification.getSigu(),
            visitCertification.getMethod(),
            visitCertification.getCertifiedAt(),
            visitCertification.getLastCertifiedAt()
        );
    }
}