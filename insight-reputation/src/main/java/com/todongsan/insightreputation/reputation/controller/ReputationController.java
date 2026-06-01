package com.todongsan.insightreputation.reputation.controller;

import com.todongsan.insightreputation.global.response.ApiResponse;
import com.todongsan.insightreputation.reputation.controller.docs.ReputationControllerDocs;
import com.todongsan.insightreputation.reputation.dto.request.ChangeResidenceRequest;
import com.todongsan.insightreputation.reputation.dto.response.MyReputationResponse;
import com.todongsan.insightreputation.reputation.dto.response.ReputationResponse;
import com.todongsan.insightreputation.reputation.service.ReputationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reputations")
@RequiredArgsConstructor
public class ReputationController implements ReputationControllerDocs {

    private final ReputationService reputationService;

    @GetMapping("/me")
    @Override
    public ApiResponse<MyReputationResponse> getMyReputation(
            @RequestHeader("X-Member-Id") Long memberId) {
        
        MyReputationResponse response = reputationService.getMyReputation(memberId);
        return ApiResponse.success(response);
    }

    @GetMapping("/{memberId}")
    @Override
    public ApiResponse<ReputationResponse> getReputation(@PathVariable Long memberId) {
        
        ReputationResponse response = reputationService.getReputation(memberId);
        return ApiResponse.success(response);
    }

    @PutMapping("/me/residence")
    @Override
    public ApiResponse<Void> changeResidence(
            @RequestHeader("X-Member-Id") Long memberId,
            @Valid @RequestBody ChangeResidenceRequest request) {
        
        reputationService.declareResidence(memberId, request.getSido(), request.getSigu());
        return ApiResponse.success();
    }
}