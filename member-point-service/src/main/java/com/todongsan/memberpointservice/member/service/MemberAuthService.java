package com.todongsan.memberpointservice.member.service;

import com.todongsan.memberpointservice.member.dto.response.LoginResponse;
import com.todongsan.memberpointservice.member.dto.response.TokenResponse;

// 카카오 OAuth 로그인 서비스
public interface MemberAuthService {

    LoginResponse kakaoLogin(String accessToken);

    TokenResponse refresh(String refreshToken);

}
