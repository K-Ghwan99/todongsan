package com.todongsan.insightreputation.reputation.dto.response;

import com.todongsan.insightreputation.reputation.entity.Reputation;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ResidenceResponse {
    
    private String sido;
    private String sigu;
    private LocalDateTime residenceDeclaredAt;
    private LocalDateTime residenceChangedAt;
    private LocalDateTime nextChangeAvailableDate;
    
    public static ResidenceResponse from(Reputation reputation) {
        LocalDateTime nextChangeAvailableDate = null;
        
        // 변경 이력이 있으면 다음 변경 가능 날짜 계산
        if (reputation.getResidenceChangedAt() != null) {
            nextChangeAvailableDate = reputation.getResidenceChangedAt().plusDays(30);
        }
        
        return ResidenceResponse.builder()
                .sido(reputation.getResidenceSido())
                .sigu(reputation.getResidenceSigu())
                .residenceDeclaredAt(reputation.getResidenceDeclaredAt())
                .residenceChangedAt(reputation.getResidenceChangedAt())
                .nextChangeAvailableDate(nextChangeAvailableDate)
                .build();
    }
}