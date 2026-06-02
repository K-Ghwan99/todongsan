package com.todongsan.battle_service.vote.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CrossAnalysisResponse {

    private Long battleId;
    // TODO: 교차분석 상세 필드 (연령대별, 성별별, 거주지역별) — Feature 3 구현 시 상세화
    private Object data;
}
