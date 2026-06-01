package com.todongsan.insightreputation.visitcertification.service;

import com.todongsan.insightreputation.enums.VisitCertMethod;
import com.todongsan.insightreputation.global.exception.CustomException;
import com.todongsan.insightreputation.global.exception.errorcode.ErrorCode;
import com.todongsan.insightreputation.visitcertification.dto.response.VisitCertificationSummary;
import com.todongsan.insightreputation.visitcertification.entity.VisitCertification;
import com.todongsan.insightreputation.visitcertification.repository.VisitCertificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VisitCertificationService {

    private static final int CERTIFICATION_COOLDOWN_DAYS = 30;
    private static final double CERTIFICATION_RADIUS_KM = 3.0;

    private final VisitCertificationRepository visitCertificationRepository;

    public List<VisitCertification> getVisitCertificationsByMemberId(Long memberId) {
        return visitCertificationRepository.findByMemberIdOrderByCertifiedAtDesc(memberId);
    }

    public List<VisitCertificationSummary> getVisitCertificationSummariesByMemberId(Long memberId) {
        List<VisitCertification> certifications = getVisitCertificationsByMemberId(memberId);
        return VisitCertificationSummary.fromList(certifications);
    }

    @Transactional
    public VisitCertification registerGpsCertification(Long memberId, String sido, String sigu, 
                                                      BigDecimal latitude, BigDecimal longitude) {
        validateCooldown(memberId, sido, sigu);
        validateGpsLocation(latitude, longitude, sido, sigu);

        Optional<VisitCertification> existingCert = 
            visitCertificationRepository.findByMemberIdAndSidoAndSigu(memberId, sido, sigu);

        if (existingCert.isPresent()) {
            VisitCertification cert = existingCert.get();
            cert.updateGpsCertification(latitude, longitude);
            return cert;
        } else {
            VisitCertification newCert = VisitCertification.builder()
                .memberId(memberId)
                .sido(sido)
                .sigu(sigu)
                .method(VisitCertMethod.GPS)
                .latitude(latitude)
                .longitude(longitude)
                .build();
            return visitCertificationRepository.save(newCert);
        }
    }

    @Transactional
    public VisitCertification registerCommentCertification(Long memberId, String sido, String sigu,
                                                          String commentContent, Long battleId) {
        validateCooldown(memberId, sido, sigu);
        validateCommentRegion(battleId, sido, sigu);

        Optional<VisitCertification> existingCert = 
            visitCertificationRepository.findByMemberIdAndSidoAndSigu(memberId, sido, sigu);

        if (existingCert.isPresent()) {
            VisitCertification cert = existingCert.get();
            cert.updateCommentCertification(commentContent, battleId);
            return cert;
        } else {
            VisitCertification newCert = VisitCertification.builder()
                .memberId(memberId)
                .sido(sido)
                .sigu(sigu)
                .method(VisitCertMethod.COMMENT)
                .commentContent(commentContent)
                .battleId(battleId)
                .build();
            return visitCertificationRepository.save(newCert);
        }
    }

    private void validateCooldown(Long memberId, String sido, String sigu) {
        Optional<VisitCertification> existingCert = 
            visitCertificationRepository.findByMemberIdAndSidoAndSigu(memberId, sido, sigu);
        
        if (existingCert.isPresent()) {
            LocalDateTime lastCertified = existingCert.get().getLastCertifiedAt();
            LocalDateTime cooldownEnd = lastCertified.plusDays(CERTIFICATION_COOLDOWN_DAYS);
            if (LocalDateTime.now().isBefore(cooldownEnd)) {
                throw new CustomException(ErrorCode.VISIT_CERT_COOLDOWN);
            }
        }
    }

    private void validateGpsLocation(BigDecimal latitude, BigDecimal longitude, String sido, String sigu) {
        // TODO: 지역 중심 좌표 조회 및 거리 계산 로직 구현
        // 현재는 간단한 검증만 수행
        if (latitude == null || longitude == null) {
            throw new CustomException(ErrorCode.VISIT_CERT_OUT_OF_RANGE);
        }
        
        // 실제 거리 계산은 추후 구현
        double distance = calculateDistance(latitude.doubleValue(), longitude.doubleValue(), sido, sigu);
        if (distance > CERTIFICATION_RADIUS_KM) {
            throw new CustomException(ErrorCode.VISIT_CERT_OUT_OF_RANGE);
        }
    }

    private void validateCommentRegion(Long battleId, String sido, String sigu) {
        // TODO: Battle Service에서 댓글의 지역 정보 조회
        // 현재는 임시 검증
        if (battleId == null) {
            throw new CustomException(ErrorCode.VISIT_CERT_COMMENT_REGION_MISMATCH);
        }
    }

    private double calculateDistance(double lat, double lng, String sido, String sigu) {
        // TODO: 실제 거리 계산 로직 구현
        // 지역별 중심 좌표 데이터가 필요함
        return 0.0;
    }
}