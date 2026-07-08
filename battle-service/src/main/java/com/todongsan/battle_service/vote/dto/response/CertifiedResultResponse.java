package com.todongsan.battle_service.vote.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CertifiedResultResponse {

    private Long battleId;
    // TODO: 방문 인증자 필터 상세 필드 — Feature 3 구현 시 상세화
    private Object data;
}
