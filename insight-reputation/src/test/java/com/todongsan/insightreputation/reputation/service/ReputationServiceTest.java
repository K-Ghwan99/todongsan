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
import com.todongsan.insightreputation.reputation.repository.ReputationRepository;
import com.todongsan.insightreputation.visitcertification.dto.response.VisitCertificationSummary;
import com.todongsan.insightreputation.visitcertification.service.VisitCertificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReputationServiceTest {

    @InjectMocks
    private ReputationService reputationService;

    @Mock
    private ReputationRepository reputationRepository;

    @Mock
    private VisitCertificationService visitCertificationService;

    @Test
    @DisplayName("내 신뢰도 조회 - 존재하는 회원 → MyReputationResponse 반환")
    void getMyReputation_existingMember_returnsMyReputationResponse() {
        // given
        Long memberId = 1L;
        Reputation reputation = createTestReputation(memberId);
        List<VisitCertificationSummary> visitCertifications = List.of();
        
        when(reputationRepository.findByMemberId(memberId)).thenReturn(Optional.of(reputation));
        when(visitCertificationService.getVisitCertificationSummariesByMemberId(memberId)).thenReturn(visitCertifications);

        // when
        MyReputationResponse response = reputationService.getMyReputation(memberId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getMemberId()).isEqualTo(memberId);
        assertThat(response.getResidenceSido()).isEqualTo("서울");
        assertThat(response.getResidenceSigu()).isEqualTo("성동구");
        assertThat(response.getVisitCertifications()).isEqualTo(visitCertifications);
    }

    @Test
    @DisplayName("내 신뢰도 조회 - 존재하지 않는 회원 → REPUTATION_NOT_FOUND")
    void getMyReputation_nonExistentMember_throwsReputationNotFound() {
        // given
        Long memberId = 999L;
        when(reputationRepository.findByMemberId(memberId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reputationService.getMyReputation(memberId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.REPUTATION_NOT_FOUND);
    }

    @Test
    @DisplayName("타인 신뢰도 조회 - 존재하는 회원 → 민감 정보 제외 응답")
    void getReputation_existingMember_returnsReputationResponseWithoutSensitiveInfo() {
        // given
        Long memberId = 1L;
        Reputation reputation = createTestReputation(memberId);
        
        when(reputationRepository.findByMemberId(memberId)).thenReturn(Optional.of(reputation));

        // when
        ReputationResponse response = reputationService.getReputation(memberId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getMemberId()).isEqualTo(memberId);
        assertThat(response.getPredictionCount()).isEqualTo(0); // 기본값
        assertThat(response.getPredictionAccuracy()).isNotNull();
        // 민감 정보인 predictionCorrect는 ReputationResponse에 없어야 함 (DTO 설계상)
        verify(visitCertificationService, never()).getVisitCertificationSummariesByMemberId(any());
    }

    @Test
    @DisplayName("거주지역 선언 - 최초 선언 → 쿨다운 skip, 정상 저장")
    void declareResidence_firstTime_skipsCooltdownAndSaves() {
        // given
        Long memberId = 1L;
        String sido = "부산";
        String sigu = "해운대구";
        
        when(reputationRepository.findByMemberId(memberId)).thenReturn(Optional.empty());
        when(reputationRepository.save(any(Reputation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Reputation result = reputationService.declareResidence(memberId, sido, sigu);

        // then
        assertThat(result.getMemberId()).isEqualTo(memberId);
        assertThat(result.getResidenceSido()).isEqualTo(sido);
        assertThat(result.getResidenceSigu()).isEqualTo(sigu);
        assertThat(result.getResidenceDeclaredAt()).isNotNull();
        verify(reputationRepository).save(any(Reputation.class));
    }

    @Test
    @DisplayName("거주지역 변경 - 30일 경과 → activityCount=0, activityConfirmedAt=NULL 초기화")
    void declareResidence_after30Days_resetsActivityCountAndConfirmedAt() {
        // given
        Long memberId = 1L;
        String newSido = "대구";
        String newSigu = "중구";
        
        Reputation existingReputation = createTestReputation(memberId);
        existingReputation.getClass(); // 실제 객체의 메서드를 호출할 수 있도록 spy 생성을 위한 준비
        Reputation spyReputation = spy(existingReputation);
        
        // 31일 전에 변경했다고 가정 (쿨다운 경과)
        LocalDateTime pastChangeTime = LocalDateTime.now().minusDays(31);
        when(spyReputation.getResidenceChangedAt()).thenReturn(pastChangeTime);
        
        when(reputationRepository.findByMemberId(memberId)).thenReturn(Optional.of(spyReputation));

        // when
        Reputation result = reputationService.declareResidence(memberId, newSido, newSigu);

        // then
        verify(spyReputation).changeResidence(newSido, newSigu);
        assertThat(result).isEqualTo(spyReputation);
    }

    @Test
    @DisplayName("거주지역 변경 - 30일 미경과 → REPUTATION_RESIDENCE_CHANGE_COOLDOWN")
    void declareResidence_before30Days_throwsCooldownException() {
        // given
        Long memberId = 1L;
        String newSido = "인천";
        String newSigu = "연수구";
        
        Reputation existingReputation = createTestReputation(memberId);
        Reputation spyReputation = spy(existingReputation);
        
        // 29일 전에 변경했다고 가정 (쿨다운 미경과)
        LocalDateTime recentChangeTime = LocalDateTime.now().minusDays(29);
        when(spyReputation.getResidenceChangedAt()).thenReturn(recentChangeTime);
        
        when(reputationRepository.findByMemberId(memberId)).thenReturn(Optional.of(spyReputation));

        // when & then
        assertThatThrownBy(() -> reputationService.declareResidence(memberId, newSido, newSigu))
                .isInstanceOf(ResidenceChangeCooldownException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.REPUTATION_RESIDENCE_CHANGE_COOLDOWN);
        
        verify(spyReputation, never()).changeResidence(any(), any());
    }

    @Test
    @DisplayName("거주지역 변경 - 정확히 30일 경과 → 성공")
    void declareResidence_exactly30Days_succeeds() {
        // given
        Long memberId = 1L;
        String newSido = "광주";
        String newSigu = "서구";
        
        Reputation existingReputation = createTestReputation(memberId);
        Reputation spyReputation = spy(existingReputation);
        
        // 정확히 30일 전에 변경했다고 가정
        LocalDateTime exactChangeTime = LocalDateTime.now().minusDays(30).minusSeconds(1);
        when(spyReputation.getResidenceChangedAt()).thenReturn(exactChangeTime);
        
        when(reputationRepository.findByMemberId(memberId)).thenReturn(Optional.of(spyReputation));

        // when
        Reputation result = reputationService.declareResidence(memberId, newSido, newSigu);

        // then
        verify(spyReputation).changeResidence(newSido, newSigu);
        assertThat(result).isEqualTo(spyReputation);
    }

    @Test
    @DisplayName("거주지역 변경 - 29일 23시간 59분 → REPUTATION_RESIDENCE_CHANGE_COOLDOWN")
    void declareResidence_29Days23Hours59Minutes_throwsCooldownException() {
        // given
        Long memberId = 1L;
        String newSido = "울산";
        String newSigu = "중구";
        
        Reputation existingReputation = createTestReputation(memberId);
        Reputation spyReputation = spy(existingReputation);
        
        // 29일 23시간 59분 전에 변경했다고 가정 (쿨다운 미경과)
        LocalDateTime almostCooldownTime = LocalDateTime.now().minusDays(29).minusHours(23).minusMinutes(59);
        when(spyReputation.getResidenceChangedAt()).thenReturn(almostCooldownTime);
        
        when(reputationRepository.findByMemberId(memberId)).thenReturn(Optional.of(spyReputation));

        // when & then
        assertThatThrownBy(() -> reputationService.declareResidence(memberId, newSido, newSigu))
                .isInstanceOf(ResidenceChangeCooldownException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.REPUTATION_RESIDENCE_CHANGE_COOLDOWN);
    }

    // ========== 내부 API 테스트 ==========
    
    @Test
    @DisplayName("활동 업데이트 - VOTE, 거주지 일치, activityCount=0 → activityCount=1, activityScore += 10")
    void updateActivity_vote_residenceMatch_activityCount0_incrementsCountAndScore() {
        // given
        Long memberId = 1L;
        Reputation reputation = createTestReputationWithActivity(memberId, 0, null);
        ActivityUpdateRequest request = createActivityUpdateRequest(memberId, "VOTE", "서울", "성동구");
        
        when(reputationRepository.findByMemberId(memberId)).thenReturn(Optional.of(reputation));
        
        // when
        ActivityUpdateResponse response = reputationService.updateActivity(request);
        
        // then
        assertThat(response.getMemberId()).isEqualTo(memberId);
        assertThat(response.getActivityScore()).isEqualTo(10); // 기존 0 + VOTE 10
        assertThat(response.getActivityCount()).isEqualTo(1); // 증가
        assertThat(response.getActivityConfirmed()).isFalse(); // 아직 3미만
        assertThat(reputation.getActivityCount()).isEqualTo(1);
        assertThat(reputation.getActivityScore()).isEqualTo(10);
    }
    
    @Test
    @DisplayName("활동 업데이트 - VOTE, 거주지 일치, activityCount=2 → activityCount=3, activityConfirmedAt 세팅")
    void updateActivity_vote_residenceMatch_activityCount2_reachesLimitAndSetsConfirmedAt() {
        // given
        Long memberId = 1L;
        Reputation reputation = createTestReputationWithActivity(memberId, 2, null);
        ActivityUpdateRequest request = createActivityUpdateRequest(memberId, "VOTE", "서울", "성동구");
        
        when(reputationRepository.findByMemberId(memberId)).thenReturn(Optional.of(reputation));
        
        // when
        ActivityUpdateResponse response = reputationService.updateActivity(request);
        
        // then
        assertThat(response.getActivityCount()).isEqualTo(3);
        assertThat(response.getActivityConfirmed()).isTrue(); // activityConfirmedAt 세팅됨
        assertThat(reputation.getActivityCount()).isEqualTo(3);
        assertThat(reputation.getActivityConfirmedAt()).isNotNull();
    }
    
    @Test
    @DisplayName("활동 업데이트 - VOTE, 거주지 일치, 이미 confirmed → activityCount 변경 없음")
    void updateActivity_vote_residenceMatch_alreadyConfirmed_noCountChange() {
        // given
        Long memberId = 1L;
        LocalDateTime confirmedAt = LocalDateTime.now().minusDays(1);
        Reputation reputation = createTestReputationWithActivity(memberId, 3, confirmedAt);
        ActivityUpdateRequest request = createActivityUpdateRequest(memberId, "VOTE", "서울", "성동구");
        
        when(reputationRepository.findByMemberId(memberId)).thenReturn(Optional.of(reputation));
        
        // when
        ActivityUpdateResponse response = reputationService.updateActivity(request);
        
        // then
        assertThat(response.getActivityCount()).isEqualTo(3); // 변경 없음
        assertThat(response.getActivityScore()).isEqualTo(10); // 점수는 증가
        assertThat(response.getActivityConfirmed()).isTrue();
    }
    
    @Test
    @DisplayName("활동 업데이트 - VOTE, 거주지 불일치 → activityCount 변경 없음, activityScore만 증가")
    void updateActivity_vote_residenceMismatch_onlyScoreIncreases() {
        // given
        Long memberId = 1L;
        Reputation reputation = createTestReputationWithActivity(memberId, 1, null);
        // 거주지역: 서울 성동구, 활동지역: 부산 해운대구 (불일치)
        ActivityUpdateRequest request = createActivityUpdateRequest(memberId, "VOTE", "부산", "해운대구");
        
        when(reputationRepository.findByMemberId(memberId)).thenReturn(Optional.of(reputation));
        
        // when
        ActivityUpdateResponse response = reputationService.updateActivity(request);
        
        // then
        assertThat(response.getActivityCount()).isEqualTo(1); // 변경 없음
        assertThat(response.getActivityScore()).isEqualTo(10); // 점수는 증가
        assertThat(response.getActivityConfirmed()).isFalse();
    }
    
    @Test
    @DisplayName("활동 업데이트 - COMMENT → +2점")
    void updateActivity_comment_addsCorrectScore() {
        // given
        Long memberId = 1L;
        Reputation reputation = createTestReputationWithActivity(memberId, 0, null);
        ActivityUpdateRequest request = createActivityUpdateRequest(memberId, "COMMENT", "서울", "성동구");
        
        when(reputationRepository.findByMemberId(memberId)).thenReturn(Optional.of(reputation));
        
        // when
        ActivityUpdateResponse response = reputationService.updateActivity(request);
        
        // then
        assertThat(response.getActivityScore()).isEqualTo(2); // COMMENT = +2점
    }
    
    @Test
    @DisplayName("활동 업데이트 - BATTLE_APPROVED → +20점")
    void updateActivity_battleApproved_addsCorrectScore() {
        // given
        Long memberId = 1L;
        Reputation reputation = createTestReputationWithActivity(memberId, 0, null);
        ActivityUpdateRequest request = createActivityUpdateRequest(memberId, "BATTLE_APPROVED", "서울", "성동구");
        
        when(reputationRepository.findByMemberId(memberId)).thenReturn(Optional.of(reputation));
        
        // when
        ActivityUpdateResponse response = reputationService.updateActivity(request);
        
        // then
        assertThat(response.getActivityScore()).isEqualTo(20); // BATTLE_APPROVED = +20점
    }
    
    @Test
    @DisplayName("활동 업데이트 - 존재하지 않는 회원 → RESOURCE_NOT_FOUND")
    void updateActivity_nonExistentMember_throwsResourceNotFound() {
        // given
        Long memberId = 999L;
        ActivityUpdateRequest request = createActivityUpdateRequest(memberId, "VOTE", "서울", "성동구");
        
        when(reputationRepository.findByMemberId(memberId)).thenReturn(Optional.empty());
        
        // when & then
        assertThatThrownBy(() -> reputationService.updateActivity(request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }
    
    @Test
    @DisplayName("활동 업데이트 - 유효하지 않은 활동 타입 → INVALID_REQUEST")
    void updateActivity_invalidActivityType_throwsInvalidRequest() {
        // given
        Long memberId = 1L;
        Reputation reputation = createTestReputationWithActivity(memberId, 0, null);
        ActivityUpdateRequest request = createActivityUpdateRequest(memberId, "INVALID_TYPE", "서울", "성동구");
        
        when(reputationRepository.findByMemberId(memberId)).thenReturn(Optional.of(reputation));
        
        // when & then
        assertThatThrownBy(() -> reputationService.updateActivity(request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_REQUEST);
    }
    
    // ========== 예측 정확도 업데이트 테스트 ==========
    
    @Test
    @DisplayName("예측 업데이트 - isCorrect=true, 10전 7승 → count=11, correct=8, accuracy=72.72")
    void updatePrediction_correctPrediction_10battles7wins_calculatesAccuracy() {
        // given
        Long memberId = 1L;
        Long marketId = 100L;
        Reputation reputation = createTestReputationWithPrediction(memberId, 10, 7);
        PredictionUpdateRequest request = createPredictionUpdateRequest(memberId, marketId, true);
        
        when(reputationRepository.findByMemberId(memberId)).thenReturn(Optional.of(reputation));
        
        // when
        PredictionUpdateResponse response = reputationService.updatePrediction(request);
        
        // then
        assertThat(response.getMemberId()).isEqualTo(memberId);
        assertThat(response.getPredictionCount()).isEqualTo(11);
        assertThat(response.getPredictionCorrect()).isEqualTo(8);
        // 8/11 * 100 = 72.727272... → Math.floor(72.727272 * 100) / 100 = 72.72
        assertThat(response.getPredictionAccuracy()).isEqualTo(72.72);
    }
    
    @Test
    @DisplayName("예측 업데이트 - isCorrect=false → predictionCorrect 변경 없음")
    void updatePrediction_incorrectPrediction_correctCountUnchanged() {
        // given
        Long memberId = 1L;
        Long marketId = 100L;
        Reputation reputation = createTestReputationWithPrediction(memberId, 5, 3);
        PredictionUpdateRequest request = createPredictionUpdateRequest(memberId, marketId, false);
        
        when(reputationRepository.findByMemberId(memberId)).thenReturn(Optional.of(reputation));
        
        // when
        PredictionUpdateResponse response = reputationService.updatePrediction(request);
        
        // then
        assertThat(response.getPredictionCount()).isEqualTo(6); // 증가
        assertThat(response.getPredictionCorrect()).isEqualTo(3); // 변경 없음
        // 3/6 * 100 = 50.00
        assertThat(response.getPredictionAccuracy()).isEqualTo(50.0);
    }
    
    @Test
    @DisplayName("예측 업데이트 - predictionCount=0 → accuracy=0 (0 나누기 방지)")
    void updatePrediction_firstPrediction_avoidsZeroDivision() {
        // given
        Long memberId = 1L;
        Long marketId = 100L;
        Reputation reputation = createTestReputationWithPrediction(memberId, 0, 0);
        PredictionUpdateRequest request = createPredictionUpdateRequest(memberId, marketId, true);
        
        when(reputationRepository.findByMemberId(memberId)).thenReturn(Optional.of(reputation));
        
        // when
        PredictionUpdateResponse response = reputationService.updatePrediction(request);
        
        // then
        assertThat(response.getPredictionCount()).isEqualTo(1);
        assertThat(response.getPredictionCorrect()).isEqualTo(1);
        assertThat(response.getPredictionAccuracy()).isEqualTo(100.0); // 1/1 * 100 = 100
    }
    
    @Test
    @DisplayName("예측 업데이트 - 소수점 버림 확인: 72.727... → 72.72, 33.337... → 33.33")
    void updatePrediction_floorCalculation_verifyFloorBehavior() {
        // given
        Long memberId = 1L;
        Long marketId = 100L;
        
        // Test case 1: 8/11 = 72.727272... 
        Reputation reputation1 = createTestReputationWithPrediction(memberId, 10, 7);
        PredictionUpdateRequest request1 = createPredictionUpdateRequest(memberId, marketId, true);
        when(reputationRepository.findByMemberId(memberId)).thenReturn(Optional.of(reputation1));
        
        PredictionUpdateResponse response1 = reputationService.updatePrediction(request1);
        assertThat(response1.getPredictionAccuracy()).isEqualTo(72.72);
        
        // Test case 2: 1/3 = 33.333333...
        Reputation reputation2 = createTestReputationWithPrediction(memberId, 2, 0);
        PredictionUpdateRequest request2 = createPredictionUpdateRequest(memberId, marketId, true);
        when(reputationRepository.findByMemberId(memberId)).thenReturn(Optional.of(reputation2));
        
        PredictionUpdateResponse response2 = reputationService.updatePrediction(request2);
        assertThat(response2.getPredictionAccuracy()).isEqualTo(33.33); // 33.337... → 33.33 (버림)
    }
    
    @Test
    @DisplayName("예측 업데이트 - 존재하지 않는 회원 → RESOURCE_NOT_FOUND")
    void updatePrediction_nonExistentMember_throwsResourceNotFound() {
        // given
        Long memberId = 999L;
        Long marketId = 100L;
        PredictionUpdateRequest request = createPredictionUpdateRequest(memberId, marketId, true);
        
        when(reputationRepository.findByMemberId(memberId)).thenReturn(Optional.empty());
        
        // when & then
        assertThatThrownBy(() -> reputationService.updatePrediction(request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    // ========== Helper Methods ==========

    private Reputation createTestReputation(Long memberId) {
        return Reputation.builder()
                .memberId(memberId)
                .residenceSido("서울")
                .residenceSigu("성동구")
                .build();
    }
    
    private Reputation createTestReputationWithActivity(Long memberId, Integer activityCount, LocalDateTime activityConfirmedAt) {
        Reputation reputation = Reputation.builder()
                .memberId(memberId)
                .residenceSido("서울")
                .residenceSigu("성동구")
                .build();
        
        // Use reflection to set private fields for testing
        try {
            java.lang.reflect.Field activityCountField = Reputation.class.getDeclaredField("activityCount");
            activityCountField.setAccessible(true);
            activityCountField.set(reputation, activityCount);
            
            if (activityConfirmedAt != null) {
                java.lang.reflect.Field activityConfirmedAtField = Reputation.class.getDeclaredField("activityConfirmedAt");
                activityConfirmedAtField.setAccessible(true);
                activityConfirmedAtField.set(reputation, activityConfirmedAt);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to set test fields", e);
        }
        
        return reputation;
    }
    
    private Reputation createTestReputationWithPrediction(Long memberId, Integer predictionCount, Integer predictionCorrect) {
        Reputation reputation = Reputation.builder()
                .memberId(memberId)
                .residenceSido("서울")
                .residenceSigu("성동구")
                .build();
        
        // Use reflection to set private fields for testing
        try {
            java.lang.reflect.Field predictionCountField = Reputation.class.getDeclaredField("predictionCount");
            predictionCountField.setAccessible(true);
            predictionCountField.set(reputation, predictionCount);
            
            java.lang.reflect.Field predictionCorrectField = Reputation.class.getDeclaredField("predictionCorrect");
            predictionCorrectField.setAccessible(true);
            predictionCorrectField.set(reputation, predictionCorrect);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set test fields", e);
        }
        
        return reputation;
    }
    
    private ActivityUpdateRequest createActivityUpdateRequest(Long memberId, String activityType, String sido, String sigu) {
        ActivityUpdateRequest.RegionDto region = ActivityUpdateRequest.RegionDto.builder()
                .sido(sido)
                .sigu(sigu)
                .build();
        
        return ActivityUpdateRequest.builder()
                .memberId(memberId)
                .activityType(activityType)
                .region(region)
                .build();
    }
    
    private PredictionUpdateRequest createPredictionUpdateRequest(Long memberId, Long marketId, Boolean isCorrect) {
        return PredictionUpdateRequest.builder()
                .memberId(memberId)
                .marketId(marketId)
                .isCorrect(isCorrect)
                .build();
    }
}