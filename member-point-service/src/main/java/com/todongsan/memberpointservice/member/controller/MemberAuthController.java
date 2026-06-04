package com.todongsan.memberpointservice.member.controller;

import com.todongsan.memberpointservice.global.response.ApiResponse;
import com.todongsan.memberpointservice.global.security.MemberPrincipal;
import com.todongsan.memberpointservice.member.dto.request.KakaoLoginRequest;
import com.todongsan.memberpointservice.member.dto.request.TokenRefreshRequest;
import com.todongsan.memberpointservice.member.dto.response.LoginResponse;
import com.todongsan.memberpointservice.member.dto.response.TokenResponse;
import com.todongsan.memberpointservice.member.service.MemberAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// 카카오 OAuth 로그인
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberAuthController {

    private final MemberAuthService memberAuthService;

    // POST /api/v1/members/oauth/kakao
    @PostMapping("/oauth/kakao")
    public ApiResponse<LoginResponse> kakaoLogin(@RequestBody @Valid KakaoLoginRequest request) {
        LoginResponse response = memberAuthService.kakaoLogin(request.getAccessToken());
        return ApiResponse.ok(response);
    }

    // POST /api/v1/members/token/refresh
    @PostMapping("/token/refresh")
    public ApiResponse<TokenResponse> refresh(@RequestBody @Valid TokenRefreshRequest request) {
        return ApiResponse.ok(memberAuthService.refresh(request.getRefreshToken()));
    }

    // POST /api/v1/members/logout (MVP : 블랙리스트 없이 200 반환)
    @PostMapping("/logout")
    public ApiResponse<Map<String, Long>> logout(@AuthenticationPrincipal MemberPrincipal principal) {
        return ApiResponse.ok(Map.of("memberId", principal.getMemberId()));
    }
}
