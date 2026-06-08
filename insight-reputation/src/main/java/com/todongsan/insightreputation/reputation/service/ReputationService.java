package com.todongsan.insightreputation.reputation.service;

import com.todongsan.insightreputation.global.exception.CustomException;
import com.todongsan.insightreputation.global.exception.errorcode.ErrorCode;
import com.todongsan.insightreputation.reputation.exception.ResidenceChangeCooldownException;
import com.todongsan.insightreputation.reputation.dto.request.ActivityUpdateRequest;
import com.todongsan.insightreputation.reputation.dto.request.PredictionUpdateRequest;
import com.todongsan.insightreputation.reputation.dto.response.ActivityUpdateResponse;
import com.todongsan.insightreputation.reputation.dto.response.MyReputationResponse;
import com.todongsan.insightreputation.reputation.dto.response.PredictionUpdateResponse;
import com.todongsan.insightreputation.reputation.dto.response.ReputationResponse;
import com.todongsan.insightreputation.reputation.entity.Reputation;
import com.todongsan.insightreputation.reputation.entity.MarketPredictionResult;
import com.todongsan.insightreputation.reputation.repository.ReputationRepository;
import com.todongsan.insightreputation.reputation.repository.MarketPredictionResultRepository;
import com.todongsan.insightreputation.visitcertification.dto.response.VisitCertificationSummary;
import com.todongsan.insightreputation.visitcertification.service.VisitCertificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReputationService {

    private static final int RESIDENCE_CHANGE_COOLDOWN_DAYS = 30;

    private final ReputationRepository reputationRepository;
    private final MarketPredictionResultRepository marketPredictionResultRepository;
    private final VisitCertificationService visitCertificationService;

    public Reputation getReputationByMemberId(Long memberId) {
        return reputationRepository.findByMemberId(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPUTATION_NOT_FOUND));
    }

    public MyReputationResponse getMyReputation(Long memberId) {
        Reputation reputation = getReputationByMemberId(memberId);
        List<VisitCertificationSummary> visitCertifications = 
            visitCertificationService.getVisitCertificationSummariesByMemberId(memberId);
        
        return MyReputationResponse.from(reputation, visitCertifications);
    }

    public ReputationResponse getReputation(Long memberId) {
        Reputation reputation = getReputationByMemberId(memberId);
        return ReputationResponse.from(reputation);
    }

    @Transactional
    public Reputation declareResidence(Long memberId, String sido, String sigu) {
        Reputation existingReputation = reputationRepository.findByMemberId(memberId).orElse(null);
        
        if (existingReputation == null) {
            // 최초 선언 - 새 Reputation 생성
            Reputation newReputation = Reputation.builder()
                    .memberId(memberId)
                    .residenceSido(sido)
                    .residenceSigu(sigu)
                    .build();
            return reputationRepository.save(newReputation);
        } else {
            // 거주지역 변경
            // 작업지시문 60-65라인: residenceChangedAt == null이면 최초 선언(쿨다운 skip)
            if (existingReputation.getResidenceChangedAt() == null) {
                // 최초 선언 → 쿨다운 체크 skip
                existingReputation.changeResidence(sido, sigu);
            } else {
                // 변경 → residenceChangedAt + 30일 > NOW() 이면 쿨다운 에러
                LocalDateTime cooldownEndTime = existingReputation.getResidenceChangedAt().plusDays(RESIDENCE_CHANGE_COOLDOWN_DAYS);
                if (LocalDateTime.now().isBefore(cooldownEndTime)) {
                    // 작업지시문 77라인: nextChangeAvailableDate 포함 에러 응답
                    throw new ResidenceChangeCooldownException(cooldownEndTime);
                }
                existingReputation.changeResidence(sido, sigu);
            }
            return existingReputation;
        }
    }

    @Transactional
    public void updateActivityScore(Long memberId, Integer score) {
        Reputation reputation = getReputationByMemberId(memberId);
        reputation.updateActivityScore(score);
    }

    @Transactional
    public void incrementActivityCount(Long memberId) {
        Reputation reputation = getReputationByMemberId(memberId);
        reputation.incrementActivityCount();
    }

    @Transactional
    public void updatePredictionStats(Long memberId, Integer count, Integer correct) {
        Reputation reputation = getReputationByMemberId(memberId);
        reputation.updatePredictionStats(count, correct);
    }

    /**
     * 활동 점수 업데이트 (내부 API)
     * 타 서비스(Battle)에서 호출되는 내부 연계 API
     * 
     * @param request 활동 업데이트 요청
     * @return 활동 업데이트 응답
     */
    @Transactional
    public ActivityUpdateResponse updateActivity(ActivityUpdateRequest request) {
        Reputation reputation = reputationRepository.findByMemberId(request.getMemberId())
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        // 활동 타입별 점수 가져오기
        int scoreToAdd = getActivityScore(request.getActivityType());
        
        // 활동 점수 증가
        reputation.updateActivityScore(scoreToAdd);

        // 거주지역 일치 여부 확인 및 activityCount 처리
        boolean isResidenceMatched = isResidenceMatched(reputation, request.getRegion());
        boolean shouldIncrementCount = isResidenceMatched && 
                                     reputation.getActivityConfirmedAt() == null;

        if (shouldIncrementCount) {
            reputation.incrementActivityCount();
        }

        return ActivityUpdateResponse.builder()
                .memberId(reputation.getMemberId())
                .activityScore(reputation.getActivityScore())
                .activityCount(reputation.getActivityCount())
                .activityConfirmed(reputation.getActivityConfirmedAt() != null)
                .build();
    }

    /**
     * 예측 정확도 업데이트 (내부 API)
     * 타 서비스(Market)에서 호출되는 내부 연계 API
     * 멱등성 보장: 동일 memberId + marketId 재시도 시 중복 처리 없이 기존 결과 반환
     * 
     * @param request 예측 업데이트 요청
     * @return 예측 업데이트 응답
     */
    @Transactional
    public PredictionUpdateResponse updatePrediction(PredictionUpdateRequest request) {
        Reputation reputation = reputationRepository.findByMemberId(request.getMemberId())
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        // 멱등성 체크: 기존 처리 결과가 있으면 중복 처리 없이 현재 상태 반환
        if (marketPredictionResultRepository.existsByMemberIdAndMarketId(request.getMemberId(), request.getMarketId())) {
            return PredictionUpdateResponse.builder()
                    .memberId(reputation.getMemberId())
                    .predictionCount(reputation.getPredictionCount())
                    .predictionCorrect(reputation.getPredictionCorrect())
                    .predictionAccuracy(reputation.getPredictionAccuracy())
                    .build();
        }

        // 처리 결과 기록 (멱등성 보장을 위한 중복 방지)
        MarketPredictionResult predictionResult = MarketPredictionResult.builder()
                .memberId(request.getMemberId())
                .marketId(request.getMarketId())
                .predictionId(request.getPredictionId())
                .isCorrect(request.getIsCorrect())
                .build();
        
        marketPredictionResultRepository.save(predictionResult);

        // 예측 카운트 증가
        int newPredictionCount = reputation.getPredictionCount() + 1;
        
        // 정답일 경우 정답 카운트 증가
        int newPredictionCorrect = reputation.getPredictionCorrect();
        if (Boolean.TRUE.equals(request.getIsCorrect())) {
            newPredictionCorrect += 1;
        }

        // 정확도 계산 (소수점 버림)
        double predictionAccuracy = 0.0;
        if (newPredictionCount > 0) {
            predictionAccuracy = Math.floor((double) newPredictionCorrect * 100 * 100 / newPredictionCount) / 100;
        }

        // 동일 트랜잭션으로 모든 필드 업데이트
        reputation.updatePredictionStats(newPredictionCount, newPredictionCorrect);
        reputation.updatePredictionAccuracy(predictionAccuracy);

        return PredictionUpdateResponse.builder()
                .memberId(reputation.getMemberId())
                .predictionCount(newPredictionCount)
                .predictionCorrect(newPredictionCorrect)
                .predictionAccuracy(predictionAccuracy)
                .build();
    }

    /**
     * 활동 타입별 점수 반환
     */
    private int getActivityScore(String activityType) {
        return switch (activityType) {
            case "VOTE" -> 10;
            case "COMMENT" -> 2;
            case "BATTLE_APPROVED" -> 20;
            default -> throw new CustomException(ErrorCode.INVALID_REQUEST);
        };
    }

    /**
     * 거주지역과 활동지역 일치 여부 확인
     */
    private boolean isResidenceMatched(Reputation reputation, ActivityUpdateRequest.RegionDto activityRegion) {
        return reputation.getResidenceSido().equals(activityRegion.getSido()) &&
               reputation.getResidenceSigu().equals(activityRegion.getSigu());
    }
}