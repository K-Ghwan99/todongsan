package com.todongsan.memberpointservice.global.security;

import com.todongsan.memberpointservice.member.entity.MemberRole;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

// SecurityContext에 저장되는 인증 주체 (JWT 검증 후 생성)
@Getter
@RequiredArgsConstructor
public class MemberPrincipal {

    private final Long memberId;
    private final MemberRole role;
}
