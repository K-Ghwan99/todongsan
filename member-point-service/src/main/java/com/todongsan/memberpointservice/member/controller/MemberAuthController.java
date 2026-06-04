package com.todongsan.memberpointservice.member.controller;

import com.todongsan.memberpointservice.global.response.ApiResponse;
import com.todongsan.memberpointservice.member.dto.request.KakaoLoginRequest;
import com.todongsan.memberpointservice.member.dto.response.LoginResponse;
import com.todongsan.memberpointservice.member.service.MemberAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
}
