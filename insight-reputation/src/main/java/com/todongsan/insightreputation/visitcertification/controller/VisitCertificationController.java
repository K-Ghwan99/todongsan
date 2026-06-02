package com.todongsan.insightreputation.visitcertification.controller;

import com.todongsan.insightreputation.global.response.ApiResponse;
import com.todongsan.insightreputation.visitcertification.controller.docs.VisitCertificationControllerDocs;
import com.todongsan.insightreputation.visitcertification.dto.VisitCertificationListResponse;
import com.todongsan.insightreputation.visitcertification.dto.VisitCertificationRequest;
import com.todongsan.insightreputation.visitcertification.dto.VisitCertificationResponse;
import com.todongsan.insightreputation.visitcertification.dto.request.CommentCertificationRequest;
import com.todongsan.insightreputation.visitcertification.dto.request.GpsCertificationRequest;
import com.todongsan.insightreputation.visitcertification.entity.VisitCertification;
import com.todongsan.insightreputation.visitcertification.service.VisitCertificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/reputations/visit-certifications")
@RequiredArgsConstructor
public class VisitCertificationController implements VisitCertificationControllerDocs {

    private final VisitCertificationService visitCertificationService;

    @Override
    @PostMapping
    public ApiResponse<VisitCertificationResponse> registerVisitCertification(
            @RequestHeader("X-Member-Id") Long memberId,
            @Valid @RequestBody VisitCertificationRequest request) {
        
        VisitCertificationResponse response = visitCertificationService.registerVisitCertification(memberId, request);
        return ApiResponse.success(response);
    }

    @Override
    @GetMapping("/mine")
    public ApiResponse<List<VisitCertificationListResponse>> getMyVisitCertifications(
            @RequestHeader("X-Member-Id") Long memberId) {
        
        log.info("내 방문 인증 목록 조회: memberId={}", memberId);

        List<VisitCertificationListResponse> certifications = 
                visitCertificationService.getMyVisitCertifications(memberId);

        return ApiResponse.success(certifications);
    }

    // Legacy endpoints (keep for backwards compatibility)
    @PostMapping("/gps")
    public ApiResponse<com.todongsan.insightreputation.visitcertification.dto.response.VisitCertificationResponse> registerGpsCertification(
            @RequestHeader("X-Member-Id") Long memberId,
            @Valid @RequestBody GpsCertificationRequest request) {
        
        VisitCertification certification = visitCertificationService.registerGpsCertification(
            memberId, 
            request.getSido(), 
            request.getSigu(), 
            request.getLatitude(), 
            request.getLongitude()
        );
        
        com.todongsan.insightreputation.visitcertification.dto.response.VisitCertificationResponse response = 
                com.todongsan.insightreputation.visitcertification.dto.response.VisitCertificationResponse.from(certification);
        
        return ApiResponse.success(response);
    }

    @PostMapping("/comment")
    public ApiResponse<com.todongsan.insightreputation.visitcertification.dto.response.VisitCertificationResponse> registerCommentCertification(
            @RequestHeader("X-Member-Id") Long memberId,
            @Valid @RequestBody CommentCertificationRequest request) {
        
        VisitCertification certification = visitCertificationService.registerCommentCertification(
            memberId,
            request.getSido(),
            request.getSigu(),
            request.getCommentId()
        );
        
        com.todongsan.insightreputation.visitcertification.dto.response.VisitCertificationResponse response = 
                com.todongsan.insightreputation.visitcertification.dto.response.VisitCertificationResponse.from(certification);
        
        return ApiResponse.success(response);
    }
}