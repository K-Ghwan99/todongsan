package com.todongsan.memberpointservice.point.controller;

import com.todongsan.memberpointservice.global.response.ApiResponse;
import com.todongsan.memberpointservice.global.security.MemberPrincipal;
import com.todongsan.memberpointservice.point.dto.response.PointBalanceResponse;
import com.todongsan.memberpointservice.point.dto.response.PointHistoryPageResponse;
import com.todongsan.memberpointservice.point.service.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/points")
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;

    @GetMapping("/balance")
    public ApiResponse<PointBalanceResponse> getBalance(
            @AuthenticationPrincipal MemberPrincipal principal) {
        return ApiResponse.ok(pointService.getBalance(principal.getMemberId()));
    }

    @GetMapping("/history")
    public ApiResponse<PointHistoryPageResponse> getHistory(
            @AuthenticationPrincipal MemberPrincipal principal,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(pointService.getHistory(principal.getMemberId(), type, page, size));
    }

}
