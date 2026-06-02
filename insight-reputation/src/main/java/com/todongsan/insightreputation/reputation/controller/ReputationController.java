package com.todongsan.insightreputation.reputation.controller;

import com.todongsan.insightreputation.global.response.ApiResponse;
import com.todongsan.insightreputation.reputation.controller.docs.ReputationControllerDocs;
import com.todongsan.insightreputation.reputation.dto.request.ActivityUpdateRequest;
import com.todongsan.insightreputation.reputation.dto.request.ChangeResidenceRequest;
import com.todongsan.insightreputation.reputation.dto.request.PredictionUpdateRequest;
import com.todongsan.insightreputation.reputation.dto.response.ActivityUpdateResponse;
import com.todongsan.insightreputation.reputation.dto.response.MyReputationResponse;
import com.todongsan.insightreputation.reputation.dto.response.PredictionUpdateResponse;
import com.todongsan.insightreputation.reputation.dto.response.ReputationResponse;
import com.todongsan.insightreputation.reputation.dto.response.ResidenceResponse;
import com.todongsan.insightreputation.reputation.entity.Reputation;
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
    public ApiResponse<ResidenceResponse> changeResidence(
            @RequestHeader("X-Member-Id") Long memberId,
            @Valid @RequestBody ChangeResidenceRequest request) {
        
        Reputation reputation = reputationService.declareResidence(memberId, request.getSido(), request.getSigu());
        ResidenceResponse response = ResidenceResponse.from(reputation);
        return ApiResponse.success(response);
    }

    /**
     * 활동 점수 업데이트 (내부 API)
     * Battle Service에서 호출하는 내부 연계 API
     */
    @PostMapping("/activity")
    public ApiResponse<ActivityUpdateResponse> updateActivity(
            @Valid @RequestBody ActivityUpdateRequest request) {
        
        ActivityUpdateResponse response = reputationService.updateActivity(request);
        return ApiResponse.success(response);
    }

    /**
     * 예측 정확도 업데이트 (내부 API)  
     * Market Service에서 호출하는 내부 연계 API
     */
    @PostMapping("/prediction")
    public ApiResponse<PredictionUpdateResponse> updatePrediction(
            @Valid @RequestBody PredictionUpdateRequest request) {
        
        PredictionUpdateResponse response = reputationService.updatePrediction(request);
        return ApiResponse.success(response);
    }
}