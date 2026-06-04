package com.todongsan.memberpointservice.global.security;

import com.todongsan.memberpointservice.member.entity.MemberRole;
import org.springframework.stereotype.Component;

// TODO 5에서 실제 JWT 구현으로 교체
@Component
public class JwtProviderStub implements JwtProvider {

    @Override
    public String generateAccessToken(Long memberId, MemberRole role) {
        return "stub-access-token-" + memberId;
    }

    @Override
    public String generateRefreshToken(Long memberId) {
        return "stub-refresh-token-" + memberId;
    }

}
