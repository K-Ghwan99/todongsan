package com.todongsan.insightreputation.reputation.controller;

import com.todongsan.insightreputation.global.response.ApiResponse;
import com.todongsan.insightreputation.reputation.dto.request.ActivityUpdateRequest;
import com.todongsan.insightreputation.reputation.dto.request.PredictionUpdateRequest;
import com.todongsan.insightreputation.reputation.dto.response.ActivityUpdateResponse;
import com.todongsan.insightreputation.reputation.dto.response.PredictionUpdateResponse;
import com.todongsan.insightreputation.reputation.service.ReputationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 내부 서비스 간 연계용 Reputation Controller
 */
@RestController
@RequestMapping("/internal/api/v1/reputations")
@RequiredArgsConstructor
public class InternalReputationController {

    private final ReputationService reputationService;

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