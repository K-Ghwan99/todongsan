package com.todongsan.battle_service.vote.controller;

import com.todongsan.battle_service.global.exception.CustomException;
import com.todongsan.battle_service.global.exception.ErrorCode;
import com.todongsan.battle_service.global.response.ApiResponse;
import com.todongsan.battle_service.vote.dto.request.VoteRequest;
import com.todongsan.battle_service.vote.dto.response.*;
import com.todongsan.battle_service.vote.service.VoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/battles")
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;

    // POST /api/v1/battles/{battleId}/votes
    @PostMapping("/{battleId}/votes")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<VoteResponse> vote(
            @PathVariable Long battleId,
            @RequestHeader("X-Member-Id") Long memberId,
            @Valid @RequestBody VoteRequest request) {
        return ApiResponse.ok(voteService.vote(battleId, memberId, request));
    }

    // GET /api/v1/battles/{battleId}/result
    @GetMapping("/{battleId}/result")
    public ApiResponse<VoteResultResponse> getResult(
            @PathVariable Long battleId,
            @RequestHeader(value = "X-Member-Id", required = false) Long memberId) {
        return ApiResponse.ok(voteService.getResult(battleId, memberId));
    }

    // GET /api/v1/battles/{battleId}/result/cross (관리자 전용)
    @GetMapping("/{battleId}/result/cross")
    public ApiResponse<CrossAnalysisResponse> getCrossResult(
            @PathVariable Long battleId,
            @RequestHeader("X-Member-Role") String role) {
        validateAdmin(role);
        return ApiResponse.ok(voteService.getCrossResult(battleId));
    }

    // GET /api/v1/battles/{battleId}/result/certified (관리자 전용)
    @GetMapping("/{battleId}/result/certified")
    public ApiResponse<CertifiedResultResponse> getCertifiedResult(
            @PathVariable Long battleId,
            @RequestHeader("X-Member-Role") String role) {
        validateAdmin(role);
        return ApiResponse.ok(voteService.getCertifiedResult(battleId));
    }

    private void validateAdmin(String role) {
        if (!"ADMIN".equals(role)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }
}
