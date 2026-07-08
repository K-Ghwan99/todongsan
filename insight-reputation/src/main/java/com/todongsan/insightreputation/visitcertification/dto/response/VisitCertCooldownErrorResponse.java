package com.todongsan.insightreputation.visitcertification.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class VisitCertCooldownErrorResponse {
    private LocalDateTime nextAvailableDate;
}