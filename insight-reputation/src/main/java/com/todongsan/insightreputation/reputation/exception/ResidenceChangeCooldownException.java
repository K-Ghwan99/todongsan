package com.todongsan.insightreputation.reputation.exception;

import com.todongsan.insightreputation.global.exception.errorcode.ErrorCode;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ResidenceChangeCooldownException extends RuntimeException {
    
    private final ErrorCode errorCode = ErrorCode.REPUTATION_RESIDENCE_CHANGE_COOLDOWN;
    private final LocalDateTime nextChangeAvailableDate;
    
    public ResidenceChangeCooldownException(LocalDateTime nextChangeAvailableDate) {
        super(ErrorCode.REPUTATION_RESIDENCE_CHANGE_COOLDOWN.getMessage());
        this.nextChangeAvailableDate = nextChangeAvailableDate;
    }
}