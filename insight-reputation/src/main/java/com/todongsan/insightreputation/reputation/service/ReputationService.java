package com.todongsan.insightreputation.reputation.service;

import com.todongsan.insightreputation.global.exception.CustomException;
import com.todongsan.insightreputation.global.exception.errorcode.ErrorCode;
import com.todongsan.insightreputation.reputation.exception.ResidenceChangeCooldownException;
import com.todongsan.insightreputation.reputation.dto.response.MyReputationResponse;
import com.todongsan.insightreputation.reputation.dto.response.ReputationResponse;
import com.todongsan.insightreputation.reputation.entity.Reputation;
import com.todongsan.insightreputation.reputation.repository.ReputationRepository;
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
}