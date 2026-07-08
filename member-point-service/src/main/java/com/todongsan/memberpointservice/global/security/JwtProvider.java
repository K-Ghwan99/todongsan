package com.todongsan.memberpointservice.global.security;

import com.todongsan.memberpointservice.member.entity.MemberRole;

public interface JwtProvider {

    String generateAccessToken(Long memberId, MemberRole role);

    String generateRefreshToken(Long memberId);

    // 토큰에서 memberId 추출 (subject 파싱)
    Long extractMemberId(String token);

    // 토큰에서 role 추출
    MemberRole extractRole(String token);

    // 유호성 검증 - 만료/위조 시 CustimException throw
    void validateToken(String token);
}
