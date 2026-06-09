package com.todongsan.insightreputation.reputation.service;

import com.todongsan.insightreputation.reputation.dto.request.PredictionUpdateRequest;
import com.todongsan.insightreputation.reputation.dto.response.PredictionUpdateResponse;
import com.todongsan.insightreputation.reputation.entity.MarketPredictionResult;
import com.todongsan.insightreputation.reputation.entity.Reputation;
import com.todongsan.insightreputation.reputation.repository.MarketPredictionResultRepository;
import com.todongsan.insightreputation.reputation.repository.ReputationRepository;
import com.todongsan.insightreputation.visitcertification.service.VisitCertificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReputationServiceIdempotencyTest {

    @InjectMocks
    private ReputationService reputationService;

    @Mock
    private ReputationRepository reputationRepository;

    @Mock
    private MarketPredictionResultRepository marketPredictionResultRepository;

    @Mock
    private VisitCertificationService visitCertificationService;

    @Test
    @DisplayName("예측 업데이트 - 최초 처리 시 정상 업데이트")
    void updatePrediction_firstTime_updatesSuccessfully() {
        // given
        Long memberId = 1L;
        Long marketId = 10L;
        Long predictionId = 100L;
        
        Reputation reputation = Reputation.builder()
                .memberId(memberId)
                .build();
        reputation.updatePredictionStats(5, 3);  // 기존 5회 중 3회 정답
        reputation.updatePredictionAccuracy(java.math.BigDecimal.valueOf(60.0));
        
        PredictionUpdateRequest request = PredictionUpdateRequest.builder()
                .memberId(memberId)
                .marketId(marketId)
                .predictionId(predictionId)
                .isCorrect(true)
                .build();

        given(reputationRepository.findByMemberId(memberId))
                .willReturn(Optional.of(reputation));
        given(marketPredictionResultRepository.existsByMemberIdAndMarketId(memberId, marketId))
                .willReturn(false);
        given(marketPredictionResultRepository.save(any(MarketPredictionResult.class)))
                .willReturn(MarketPredictionResult.builder()
                        .memberId(memberId)
                        .marketId(marketId)
                        .predictionId(predictionId)
                        .isCorrect(true)
                        .build());

        // when
        PredictionUpdateResponse response = reputationService.updatePrediction(request);

        // then
        assertThat(response.getMemberId()).isEqualTo(memberId);
        assertThat(response.getPredictionCount()).isEqualTo(6);  // 5 + 1
        assertThat(response.getPredictionCorrect()).isEqualTo(4);  // 3 + 1 (정답)
        assertThat(response.getPredictionAccuracy().compareTo(java.math.BigDecimal.valueOf(66.66))).isEqualTo(0);  // FLOOR(4/6 * 100 * 100) / 100

        // 처리 결과 기록 및 reputation 업데이트 확인
        verify(marketPredictionResultRepository, times(1)).save(any(MarketPredictionResult.class));
        assertThat(reputation.getPredictionCount()).isEqualTo(6);
        assertThat(reputation.getPredictionCorrect()).isEqualTo(4);
    }

    @Test
    @DisplayName("예측 업데이트 - 재시도 시 멱등성 보장 (중복 처리 없음)")
    void updatePrediction_retryRequest_idempotencyGuaranteed() {
        // given
        Long memberId = 1L;
        Long marketId = 10L;
        Long predictionId = 100L;
        
        Reputation reputation = Reputation.builder()
                .memberId(memberId)
                .build();
        reputation.updatePredictionStats(6, 4);  // 이미 처리된 상태
        reputation.updatePredictionAccuracy(java.math.BigDecimal.valueOf(66.66));
        
        PredictionUpdateRequest request = PredictionUpdateRequest.builder()
                .memberId(memberId)
                .marketId(marketId)
                .predictionId(predictionId)
                .isCorrect(true)
                .build();

        given(reputationRepository.findByMemberId(memberId))
                .willReturn(Optional.of(reputation));
        given(marketPredictionResultRepository.existsByMemberIdAndMarketId(memberId, marketId))
                .willReturn(true);  // 이미 처리된 상태

        // when
        PredictionUpdateResponse response = reputationService.updatePrediction(request);

        // then
        assertThat(response.getMemberId()).isEqualTo(memberId);
        assertThat(response.getPredictionCount()).isEqualTo(6);  // 기존 상태 그대로
        assertThat(response.getPredictionCorrect()).isEqualTo(4);  // 기존 상태 그대로
        assertThat(response.getPredictionAccuracy().compareTo(java.math.BigDecimal.valueOf(66.66))).isEqualTo(0);  // 기존 상태 그대로

        // 중복 처리 없음 확인
        verify(marketPredictionResultRepository, never()).save(any(MarketPredictionResult.class));
        assertThat(reputation.getPredictionCount()).isEqualTo(6);  // 변경 없음
        assertThat(reputation.getPredictionCorrect()).isEqualTo(4);  // 변경 없음
    }

    @Test
    @DisplayName("예측 업데이트 - 오답 처리 시 정답 카운트 증가 없음")
    void updatePrediction_incorrectAnswer_noCorrectCountIncrease() {
        // given
        Long memberId = 1L;
        Long marketId = 10L;
        
        Reputation reputation = Reputation.builder()
                .memberId(memberId)
                .build();
        reputation.updatePredictionStats(5, 3);  // 기존 5회 중 3회 정답
        reputation.updatePredictionAccuracy(java.math.BigDecimal.valueOf(60.0));
        
        PredictionUpdateRequest request = PredictionUpdateRequest.builder()
                .memberId(memberId)
                .marketId(marketId)
                .isCorrect(false)  // 오답
                .build();

        given(reputationRepository.findByMemberId(memberId))
                .willReturn(Optional.of(reputation));
        given(marketPredictionResultRepository.existsByMemberIdAndMarketId(memberId, marketId))
                .willReturn(false);
        given(marketPredictionResultRepository.save(any(MarketPredictionResult.class)))
                .willReturn(MarketPredictionResult.builder()
                        .memberId(memberId)
                        .marketId(marketId)
                        .isCorrect(false)
                        .build());

        // when
        PredictionUpdateResponse response = reputationService.updatePrediction(request);

        // then
        assertThat(response.getMemberId()).isEqualTo(memberId);
        assertThat(response.getPredictionCount()).isEqualTo(6);  // 5 + 1
        assertThat(response.getPredictionCorrect()).isEqualTo(3);  // 3 (정답 카운트 증가 없음)
        assertThat(response.getPredictionAccuracy().compareTo(java.math.BigDecimal.valueOf(50.0))).isEqualTo(0);  // FLOOR(3/6 * 100 * 100) / 100

        verify(marketPredictionResultRepository, times(1)).save(any(MarketPredictionResult.class));
    }

    @Test
    @DisplayName("예측 업데이트 - predictionId 없이도 정상 처리")
    void updatePrediction_noPredictionId_processesNormally() {
        // given
        Long memberId = 1L;
        Long marketId = 10L;
        
        Reputation reputation = Reputation.builder()
                .memberId(memberId)
                .build();
        reputation.updatePredictionStats(0, 0);  // 초기 상태
        reputation.updatePredictionAccuracy(java.math.BigDecimal.valueOf(0.0));
        
        PredictionUpdateRequest request = PredictionUpdateRequest.builder()
                .memberId(memberId)
                .marketId(marketId)
                .predictionId(null)  // predictionId 없음
                .isCorrect(true)
                .build();

        given(reputationRepository.findByMemberId(memberId))
                .willReturn(Optional.of(reputation));
        given(marketPredictionResultRepository.existsByMemberIdAndMarketId(memberId, marketId))
                .willReturn(false);
        given(marketPredictionResultRepository.save(any(MarketPredictionResult.class)))
                .willReturn(MarketPredictionResult.builder()
                        .memberId(memberId)
                        .marketId(marketId)
                        .predictionId(null)
                        .isCorrect(true)
                        .build());

        // when
        PredictionUpdateResponse response = reputationService.updatePrediction(request);

        // then
        assertThat(response.getMemberId()).isEqualTo(memberId);
        assertThat(response.getPredictionCount()).isEqualTo(1);
        assertThat(response.getPredictionCorrect()).isEqualTo(1);
        assertThat(response.getPredictionAccuracy().compareTo(java.math.BigDecimal.valueOf(100.0))).isEqualTo(0);

        verify(marketPredictionResultRepository, times(1)).save(any(MarketPredictionResult.class));
    }
}