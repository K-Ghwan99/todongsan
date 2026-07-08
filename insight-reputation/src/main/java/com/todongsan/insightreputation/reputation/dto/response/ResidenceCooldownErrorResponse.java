package com.todongsan.insightreputation.reputation.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ResidenceCooldownErrorResponse {
    private LocalDateTime nextChangeAvailableDate;
}