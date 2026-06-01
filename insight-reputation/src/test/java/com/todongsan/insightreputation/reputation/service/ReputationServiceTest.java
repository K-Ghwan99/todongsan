package com.todongsan.insightreputation.reputation.service;

import com.todongsan.insightreputation.global.exception.CustomException;
import com.todongsan.insightreputation.global.exception.errorcode.ErrorCode;
import com.todongsan.insightreputation.reputation.dto.response.MyReputationResponse;
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
                .isInstanceOf(CustomException.class)
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
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.REPUTATION_RESIDENCE_CHANGE_COOLDOWN);
    }

    private Reputation createTestReputation(Long memberId) {
        return Reputation.builder()
                .memberId(memberId)
                .residenceSido("서울")
                .residenceSigu("성동구")
                .build();
    }
}