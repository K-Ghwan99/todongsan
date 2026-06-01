package com.todongsan.insightreputation.visitcertification.controller;

import com.todongsan.insightreputation.global.response.ApiResponse;
import com.todongsan.insightreputation.visitcertification.dto.request.CommentCertificationRequest;
import com.todongsan.insightreputation.visitcertification.dto.request.GpsCertificationRequest;
import com.todongsan.insightreputation.visitcertification.dto.response.VisitCertificationResponse;
import com.todongsan.insightreputation.visitcertification.entity.VisitCertification;
import com.todongsan.insightreputation.visitcertification.service.VisitCertificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reputations/visit-certifications")
@RequiredArgsConstructor
public class VisitCertificationController {

    private final VisitCertificationService visitCertificationService;

    @PostMapping("/gps")
    public ApiResponse<VisitCertificationResponse> registerGpsCertification(
            @RequestHeader("X-Member-Id") Long memberId,
            @Valid @RequestBody GpsCertificationRequest request) {
        
        VisitCertification certification = visitCertificationService.registerGpsCertification(
            memberId, 
            request.getSido(), 
            request.getSigu(), 
            request.getLatitude(), 
            request.getLongitude()
        );
        
        VisitCertificationResponse response = VisitCertificationResponse.from(certification);
        
        return ApiResponse.success(response);
    }

    @PostMapping("/comment")
    public ApiResponse<VisitCertificationResponse> registerCommentCertification(
            @RequestHeader("X-Member-Id") Long memberId,
            @Valid @RequestBody CommentCertificationRequest request) {
        
        VisitCertification certification = visitCertificationService.registerCommentCertification(
            memberId,
            request.getSido(),
            request.getSigu(),
            request.getCommentContent(),
            request.getBattleId()
        );
        
        VisitCertificationResponse response = VisitCertificationResponse.from(certification);
        
        return ApiResponse.success(response);
    }
}