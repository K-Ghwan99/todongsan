package com.todongsan.insightreputation.reputation.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PredictionUpdateRequest {

    @NotNull(message = "회원 ID는 필수입니다")
    private Long memberId;

    @NotNull(message = "마켓 ID는 필수입니다")
    private Long marketId;

    @NotNull(message = "정답 여부는 필수입니다")
    private Boolean isCorrect;
}