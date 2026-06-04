package com.todongsan.insightreputation.visitcertification.exception;

import com.todongsan.insightreputation.global.exception.errorcode.ErrorCode;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class VisitCertCooldownException extends RuntimeException {
    
    private final ErrorCode errorCode = ErrorCode.VISIT_CERT_COOLDOWN;
    private final LocalDateTime nextAvailableDate;
    
    public VisitCertCooldownException(LocalDateTime nextAvailableDate) {
        super(ErrorCode.VISIT_CERT_COOLDOWN.getMessage());
        this.nextAvailableDate = nextAvailableDate;
    }
}