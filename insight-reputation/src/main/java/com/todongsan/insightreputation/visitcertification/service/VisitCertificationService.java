package com.todongsan.insightreputation.visitcertification.service;

import com.todongsan.insightreputation.enums.VisitCertMethod;
import com.todongsan.insightreputation.global.exception.CustomException;
import com.todongsan.insightreputation.global.exception.errorcode.ErrorCode;
import com.todongsan.insightreputation.visitcertification.dto.VisitCertificationListResponse;
import com.todongsan.insightreputation.visitcertification.dto.VisitCertificationResponse;
import com.todongsan.insightreputation.visitcertification.dto.response.VisitCertificationSummary;
import com.todongsan.insightreputation.visitcertification.entity.VisitCertification;
import com.todongsan.insightreputation.visitcertification.repository.VisitCertificationRepository;
import com.todongsan.insightreputation.visitcertification.util.GpsDistanceCalculator;
import com.todongsan.insightreputation.visitcertification.util.RegionCenterCoordinateProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VisitCertificationService {

    private static final int CERTIFICATION_COOLDOWN_DAYS = 30;
    private static final double CERTIFICATION_RADIUS_KM = 3.0;

    private final VisitCertificationRepository visitCertificationRepository;
    private final RegionCenterCoordinateProvider regionCoordinateProvider;

    public List<VisitCertification> getVisitCertificationsByMemberId(Long memberId) {
        return visitCertificationRepository.findByMemberIdOrderByCertifiedAtDesc(memberId);
    }

    public List<VisitCertificationSummary> getVisitCertificationSummariesByMemberId(Long memberId) {
        List<VisitCertification> certifications = getVisitCertificationsByMemberId(memberId);
        return VisitCertificationSummary.fromList(certifications);
    }

    /**
     * 회원의 모든 방문 인증 목록을 조회합니다.
     * 
     * @param memberId 회원 ID
     * @return 방문 인증 목록
     */
    public List<VisitCertificationListResponse> getMyVisitCertifications(Long memberId) {
        List<VisitCertification> certifications = visitCertificationRepository
                .findByMemberIdOrderByCertifiedAtDesc(memberId);
        
        return certifications.stream()
                .map(VisitCertificationListResponse::from)
                .toList();
    }

    /**
     * GPS 기반 방문 인증을 등록합니다.
     * 
     * @param memberId 회원 ID
     * @param sido 시/도
     * @param sigu 시/구
     * @param latitude 사용자 위도
     * @param longitude 사용자 경도
     * @return 방문 인증 응답
     */
    @Transactional
    public VisitCertificationResponse registerGps(Long memberId, String sido, String sigu, 
                                                 Double latitude, Double longitude) {
        log.info("GPS 방문 인증 등록 시작: memberId={}, sido={}, sigu={}, lat={}, lon={}", 
                memberId, sido, sigu, latitude, longitude);

        // 1. 기존 인증 조회
        Optional<VisitCertification> existingCert = visitCertificationRepository
                .findByMemberIdAndSidoAndSigu(memberId, sido, sigu);

        // 2. 쿨다운 체크 (기존 인증 존재 시)
        if (existingCert.isPresent()) {
            LocalDateTime lastCertified = existingCert.get().getLastCertifiedAt();
            LocalDateTime cooldownExpiry = lastCertified.plusDays(30);
            
            if (LocalDateTime.now().isBefore(cooldownExpiry)) {
                log.warn("방문 인증 쿨다운 미경과: memberId={}, lastCertified={}, cooldownExpiry={}", 
                        memberId, lastCertified, cooldownExpiry);
                throw new CustomException(ErrorCode.VISIT_CERT_COOLDOWN);
            }
        }

        // 3. 지역 중심 좌표 조회
        RegionCenterCoordinateProvider.RegionCoordinate centerCoord = regionCoordinateProvider
                .getCoordinate(sido, sigu)
                .orElseThrow(() -> {
                    log.warn("지원하지 않는 지역: sido={}, sigu={}", sido, sigu);
                    return new CustomException(ErrorCode.VISIT_CERT_UNSUPPORTED_REGION);
                });

        // 4. 거리 계산 및 반경 체크
        boolean withinRadius = GpsDistanceCalculator.isWithinRadius(
                latitude, longitude, 
                centerCoord.getLatitude(), centerCoord.getLongitude()
        );

        if (!withinRadius) {
            double distance = GpsDistanceCalculator.calculateDistance(
                    latitude, longitude, 
                    centerCoord.getLatitude(), centerCoord.getLongitude()
            );
            log.warn("GPS 인증 반경 초과: memberId={}, distance={}km", memberId, distance);
            throw new CustomException(ErrorCode.VISIT_CERT_OUT_OF_RANGE);
        }

        // 5. 인증 저장 (신규 또는 업데이트)
        VisitCertification savedCert;
        if (existingCert.isPresent()) {
            // 재인증: 기존 레코드 업데이트
            VisitCertification cert = existingCert.get();
            cert.updateGpsCertification(new BigDecimal(latitude.toString()), new BigDecimal(longitude.toString()));
            savedCert = cert;
            log.info("GPS 재인증 완료: memberId={}, sido={}, sigu={}", memberId, sido, sigu);
        } else {
            // 최초 인증: 새 레코드 생성
            VisitCertification newCert = VisitCertification.builder()
                    .memberId(memberId)
                    .sido(sido)
                    .sigu(sigu)
                    .method(VisitCertMethod.GPS)
                    .latitude(new BigDecimal(latitude.toString()))
                    .longitude(new BigDecimal(longitude.toString()))
                    .build();
            savedCert = visitCertificationRepository.save(newCert);
            log.info("GPS 최초 인증 완료: memberId={}, sido={}, sigu={}", memberId, sido, sigu);
        }

        return VisitCertificationResponse.from(savedCert);
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
        if (latitude == null || longitude == null) {
            throw new CustomException(ErrorCode.VISIT_CERT_OUT_OF_RANGE);
        }

        // 지역 중심 좌표 조회
        RegionCenterCoordinateProvider.RegionCoordinate centerCoord = regionCoordinateProvider
                .getCoordinate(sido, sigu)
                .orElseThrow(() -> new CustomException(ErrorCode.VISIT_CERT_UNSUPPORTED_REGION));

        // 거리 계산 및 반경 체크
        boolean withinRadius = GpsDistanceCalculator.isWithinRadius(
                latitude.doubleValue(), longitude.doubleValue(), 
                centerCoord.getLatitude(), centerCoord.getLongitude()
        );

        if (!withinRadius) {
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

}