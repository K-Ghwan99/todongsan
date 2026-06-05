package com.todongsan.memberpointservice.member.controller;

import com.todongsan.memberpointservice.global.response.ApiResponse;
import com.todongsan.memberpointservice.member.dto.request.MemberBatchRequest;
import com.todongsan.memberpointservice.member.dto.response.MemberBatchItemResponse;
import com.todongsan.memberpointservice.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberInternalController {

    private final MemberService memberService;

    @PostMapping("/batch")
    public ApiResponse<List<MemberBatchItemResponse>> getBatch(@Valid @RequestBody MemberBatchRequest request) {
        return ApiResponse.ok(memberService.getBatch(request.getMemberIds()));
    }
}
