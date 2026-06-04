package com.todongsan.memberpointservice.global.security;

import com.todongsan.memberpointservice.member.entity.MemberRole;

public interface JwtProvider {

    String generateAccessToken(Long memberId, MemberRole role);

    String generateRefreshToken(Long memberId);
}
