package com.todongsan.memberpointservice.global.security;

import com.todongsan.memberpointservice.member.entity.MemberRole;

public class JwtProviderStub implements JwtProvider {

    @Override
    public String generateAccessToken(Long memberId, MemberRole role) {
        return "stub-access-token-" + memberId;
    }

    @Override
    public String generateRefreshToken(Long memberId) {
        return "stub-refresh-token-" + memberId;
    }

    @Override
    public Long extractMemberId(String token) {
        return Long.parseLong(token.replace("stub-access-token-", ""));
    }

    @Override
    public MemberRole extractRole(String token) {
        return MemberRole.USER;
    }

    @Override
    public void validateToken(String token) {
        // stub: 항상 유효한 것으로 처리
    }
}
