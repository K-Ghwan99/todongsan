package com.todongsan.memberpointservice.member.dto.response;

import lombok.Builder;
import lombok.Getter;

// 로그인 응답
@Getter
@Builder
public class LoginResponse {

    private String accessToken;
    private String refreshToken;
    private Long memberId;
    private String nickname;
    private boolean isNewMember;
}
