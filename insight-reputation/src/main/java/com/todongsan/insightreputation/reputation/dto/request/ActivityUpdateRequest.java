package com.todongsan.insightreputation.reputation.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ActivityUpdateRequest {

    @NotNull(message = "회원 ID는 필수입니다")
    private Long memberId;

    @NotBlank(message = "활동 타입은 필수입니다")
    private String activityType;

    @Valid
    @NotNull(message = "지역 정보는 필수입니다")
    private RegionDto region;

    @Data
    @Builder
    public static class RegionDto {
        
        @NotBlank(message = "시도는 필수입니다")
        private String sido;
        
        @NotBlank(message = "시구는 필수입니다")
        private String sigu;
    }
}