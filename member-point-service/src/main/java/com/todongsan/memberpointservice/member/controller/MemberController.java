package com.todongsan.memberpointservice.member.controller;

import com.todongsan.memberpointservice.global.response.ApiResponse;
import com.todongsan.memberpointservice.global.security.MemberPrincipal;
import com.todongsan.memberpointservice.member.dto.request.MemberUpdateRequest;
import com.todongsan.memberpointservice.member.dto.response.MemberResponse;
import com.todongsan.memberpointservice.member.dto.response.MemberUpdateResponse;
import com.todongsan.memberpointservice.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/me")
    public ApiResponse<MemberResponse> getMe(@AuthenticationPrincipal MemberPrincipal principal) {
        return ApiResponse.ok(memberService.getMe(principal.getMemberId()));
    }

    @PatchMapping("/me")
    public ApiResponse<MemberUpdateResponse> updateMe(
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestBody MemberUpdateRequest request) {
        return ApiResponse.ok(memberService.updateMe(principal.getMemberId(), request));
    }

    @DeleteMapping("/me")
    public ApiResponse<Map<String, Long>> withdraw(
            @AuthenticationPrincipal MemberPrincipal principal) {
        Long memberId = memberService.withdraw(principal.getMemberId());
        return ApiResponse.ok(Map.of("memberId", memberId));
    }

}
