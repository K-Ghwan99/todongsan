package com.todongsan.insightreputation.insight.service;

import com.todongsan.insightreputation.global.client.BattleClient;
import com.todongsan.insightreputation.global.client.BattleResponse;
import com.todongsan.insightreputation.global.client.ClaudeApiClient;
import com.todongsan.insightreputation.global.client.MarketClient;
import com.todongsan.insightreputation.global.client.MarketInsightSummaryResponse;
import com.todongsan.insightreputation.global.client.MemberPointClient;
import com.todongsan.insightreputation.global.exception.CustomException;
import com.todongsan.insightreputation.global.exception.errorcode.ErrorCode;
import com.todongsan.insightreputation.insight.dto.InsightReportResponse;
import com.todongsan.insightreputation.insight.dto.InsightReportStatusResponse;
import com.todongsan.insightreputation.insight.entity.InsightReport;
import com.todongsan.insightreputation.enums.InsightReportStatus;
import com.todongsan.insightreputation.enums.InsightReportType;
import com.todongsan.insightreputation.insight.repository.InsightReportRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InsightReportServiceTest {

    @InjectMocks
    InsightReportService service;
    
    @Mock
    InsightReportRepository insightReportRepository;
    
    @Mock
    BattleClient battleClient;
    
    @Mock
    MarketClient marketClient;
    
    @Mock
    MemberPointClient memberPointClient;
    
    @Mock
    ClaudeApiClient claudeApiClient;

    @Test
    @DisplayName("Battle 리포트 생성 - 정상 요청 → PENDING 상태 리포트 생성")
    void requestBattleReport_validRequest_success() {
        // Given
        Long memberId = 1L;
        Long battleId = 100L;
        
        // 기존 리포트 없음
        when(insightReportRepository.findByTypeAndReferenceId(InsightReportType.BATTLE, battleId))
                .thenReturn(Optional.empty());
        
        // 종료된 Battle
        BattleResponse battleResponse = BattleResponse.builder()
                .battleId(battleId)
                .title("Test Battle")
                .isClosed(true)
                .status("CLOSED")
                .build();
        when(battleClient.getBattleInfo(battleId)).thenReturn(battleResponse);
        
        // 포인트 차감 성공
        doNothing().when(memberPointClient).spendPoints(eq(memberId), eq(80), anyString());
        
        // 새 리포트 저장 후 ID 설정을 모킹
        InsightReport savedReport = mock(InsightReport.class);
        when(savedReport.getId()).thenReturn(1L);
        when(savedReport.getStatus()).thenReturn(InsightReportStatus.PENDING);
        when(insightReportRepository.save(any(InsightReport.class))).thenReturn(savedReport);
        
        // When
        InsightReportResponse response = service.requestBattleReport(memberId, battleId, "test-key");
        
        // Then
        assertThat(response.getReportId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getPointCharged()).isEqualTo(80);
        assertThat(response.getReportContent()).isNull();
        
        verify(memberPointClient).spendPoints(eq(memberId), eq(80), anyString());
        verify(insightReportRepository).save(any(InsightReport.class));
    }
    
    @Test
    @DisplayName("Battle 리포트 생성 - 기존 DONE 리포트 존재 → 즉시 반환")
    void requestBattleReport_existingDoneReport_returnImmediately() {
        // Given
        Long memberId = 1L;
        Long battleId = 100L;
        
        InsightReport existingReport = mock(InsightReport.class);
        when(existingReport.getId()).thenReturn(1L);
        when(existingReport.getStatus()).thenReturn(InsightReportStatus.DONE);
        when(existingReport.getReportContent()).thenReturn("기존 분석 결과");
        when(existingReport.getGeneratedAt()).thenReturn(LocalDateTime.now());
        
        when(insightReportRepository.findByTypeAndReferenceId(InsightReportType.BATTLE, battleId))
                .thenReturn(Optional.of(existingReport));
        
        // When
        InsightReportResponse response = service.requestBattleReport(memberId, battleId, "test-key");
        
        // Then
        assertThat(response.getReportId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo("DONE");
        assertThat(response.getPointCharged()).isEqualTo(0);  // 포인트 차감 없음
        assertThat(response.getReportContent()).isEqualTo("기존 분석 결과");
        
        verify(memberPointClient, never()).spendPoints(anyLong(), anyInt(), anyString());
        verify(insightReportRepository, never()).save(any(InsightReport.class));
    }
    
    @Test
    @DisplayName("Battle 리포트 생성 - PROCESSING 중복 요청 → ALREADY_PROCESSING 오류")
    void requestBattleReport_alreadyProcessing_throwsException() {
        // Given
        Long memberId = 1L;
        Long battleId = 100L;
        
        InsightReport processingReport = mock(InsightReport.class);
        when(processingReport.getId()).thenReturn(1L);
        when(processingReport.getStatus()).thenReturn(InsightReportStatus.PROCESSING);
        
        when(insightReportRepository.findByTypeAndReferenceId(InsightReportType.BATTLE, battleId))
                .thenReturn(Optional.of(processingReport));
        
        // When & Then
        assertThatThrownBy(() -> service.requestBattleReport(memberId, battleId, "test-key"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INSIGHT_REPORT_ALREADY_PROCESSING);
        
        verify(memberPointClient, never()).spendPoints(anyLong(), anyInt(), anyString());
    }
    
    @Test
    @DisplayName("Battle 리포트 생성 - Battle 미종료 → SOURCE_NOT_CLOSED 오류")
    void requestBattleReport_battleNotClosed_throwsException() {
        // Given
        Long memberId = 1L;
        Long battleId = 100L;
        
        when(insightReportRepository.findByTypeAndReferenceId(InsightReportType.BATTLE, battleId))
                .thenReturn(Optional.empty());
        
        BattleResponse openBattle = BattleResponse.builder()
                .battleId(battleId)
                .title("진행중 Battle")
                .isClosed(false)
                .status("ACTIVE")
                .build();
        when(battleClient.getBattleInfo(battleId)).thenReturn(openBattle);
        
        // When & Then
        assertThatThrownBy(() -> service.requestBattleReport(memberId, battleId, "test-key"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INSIGHT_REPORT_SOURCE_NOT_CLOSED);
        
        verify(memberPointClient, never()).spendPoints(anyLong(), anyInt(), anyString());
    }
    
    @Test
    @DisplayName("Battle 리포트 생성 - 포인트 부족 → POINT_INSUFFICIENT 오류")
    void requestBattleReport_insufficientPoints_throwsException() {
        // Given
        Long memberId = 1L;
        Long battleId = 100L;
        
        when(insightReportRepository.findByTypeAndReferenceId(InsightReportType.BATTLE, battleId))
                .thenReturn(Optional.empty());
        
        BattleResponse battleResponse = BattleResponse.builder()
                .battleId(battleId)
                .isClosed(true)
                .build();
        when(battleClient.getBattleInfo(battleId)).thenReturn(battleResponse);
        
        doThrow(new CustomException(ErrorCode.POINT_INSUFFICIENT))
                .when(memberPointClient).spendPoints(eq(memberId), eq(80), anyString());
        
        // When & Then
        assertThatThrownBy(() -> service.requestBattleReport(memberId, battleId, "test-key"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POINT_INSUFFICIENT);
        
        verify(insightReportRepository, never()).save(any(InsightReport.class));
    }
    
    @Test
    @DisplayName("리포트 상태 조회 - 정상 조회")
    void getBattleReportStatus_validRequest_success() {
        // Given
        Long battleId = 100L;
        
        InsightReport report = mock(InsightReport.class);
        when(report.getId()).thenReturn(1L);
        when(report.getStatus()).thenReturn(InsightReportStatus.PROCESSING);
        when(report.getRetryCount()).thenReturn((byte) 1);
        when(report.getCreatedAt()).thenReturn(LocalDateTime.now().minusMinutes(5));
        when(report.getProcessingStartedAt()).thenReturn(LocalDateTime.now().minusMinutes(3));
        
        when(insightReportRepository.findByTypeAndReferenceId(InsightReportType.BATTLE, battleId))
                .thenReturn(Optional.of(report));
        
        // When
        InsightReportStatusResponse response = service.getBattleReportStatus(battleId);
        
        // Then
        assertThat(response.getReportId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo("PROCESSING");
        assertThat(response.getRetryCount()).isEqualTo(1);
        assertThat(response.getProcessingStartedAt()).isNotNull();
    }
    
    @Test
    @DisplayName("리포트 상태 조회 - 리포트 없음 → NOT_FOUND 오류")
    void getBattleReportStatus_reportNotFound_throwsException() {
        // Given
        Long battleId = 100L;
        
        when(insightReportRepository.findByTypeAndReferenceId(InsightReportType.BATTLE, battleId))
                .thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> service.getBattleReportStatus(battleId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INSIGHT_REPORT_NOT_FOUND);
    }
    
    @Test
    @DisplayName("PROCESSING 전이 - PENDING 상태 → PROCESSING 전이 성공")
    void transitionToPROCESSING_pendingState_success() {
        // Given
        Long reportId = 1L;
        
        InsightReport pendingReport = mock(InsightReport.class);
        when(pendingReport.getId()).thenReturn(reportId);
        when(pendingReport.getStatus()).thenReturn(InsightReportStatus.PENDING);
        
        when(insightReportRepository.findById(reportId)).thenReturn(Optional.of(pendingReport));
        when(insightReportRepository.save(any(InsightReport.class))).thenReturn(pendingReport);
        
        // When
        boolean result = service.transitionToPROCESSING(reportId);
        
        // Then
        assertThat(result).isTrue();
        verify(insightReportRepository).save(any(InsightReport.class));
    }
    
    @Test
    @DisplayName("PROCESSING 전이 - 이미 PROCESSING 상태 → 전이 실패")
    void transitionToPROCESSING_alreadyProcessing_failure() {
        // Given
        Long reportId = 1L;
        
        InsightReport processingReport = mock(InsightReport.class);
        when(processingReport.getId()).thenReturn(reportId);
        when(processingReport.getStatus()).thenReturn(InsightReportStatus.PROCESSING);
        
        when(insightReportRepository.findById(reportId)).thenReturn(Optional.of(processingReport));
        
        // When
        boolean result = service.transitionToPROCESSING(reportId);
        
        // Then
        assertThat(result).isFalse();
        verify(insightReportRepository, never()).save(any(InsightReport.class));
    }
    
    @Test
    @DisplayName("분석 완료 처리 - 결과 저장 및 DONE 전이")
    void completeAnalysis_validInput_success() {
        // Given
        Long reportId = 1L;
        String analysisResult = "분석 결과";
        
        InsightReport report = mock(InsightReport.class);
        when(report.getId()).thenReturn(reportId);
        when(report.getStatus()).thenReturn(InsightReportStatus.PROCESSING);
        
        when(insightReportRepository.findById(reportId)).thenReturn(Optional.of(report));
        when(insightReportRepository.save(any(InsightReport.class))).thenReturn(report);
        
        // When
        service.completeAnalysis(reportId, analysisResult);
        
        // Then
        verify(insightReportRepository).save(any(InsightReport.class));
    }
    
    @Test
    @DisplayName("분석 실패 처리 - 재시도 횟수 미만 → PENDING 리셋")
    void handleAnalysisFailure_belowMaxRetry_resetToPending() {
        // Given
        Long reportId = 1L;
        Exception exception = new RuntimeException("분석 실패");
        
        InsightReport report = mock(InsightReport.class);
        when(report.getId()).thenReturn(reportId);
        when(report.getRequestedBy()).thenReturn(1L);
        when(report.getStatus()).thenReturn(InsightReportStatus.PROCESSING);
        when(report.getRetryCount()).thenReturn((byte) 1);
        
        when(insightReportRepository.findById(reportId)).thenReturn(Optional.of(report));
        when(insightReportRepository.save(any(InsightReport.class))).thenReturn(report);
        
        // When
        service.handleAnalysisFailure(reportId, exception);
        
        // Then
        verify(insightReportRepository).save(any(InsightReport.class));
        verify(memberPointClient, never()).refundPoints(anyLong(), anyInt(), anyString());
    }
    
    @Test
    @DisplayName("분석 실패 처리 - 최대 재시도 초과 → 영구 FAILED + 환불")
    void handleAnalysisFailure_exceedsMaxRetry_permanentFailureAndRefund() {
        // Given
        Long reportId = 1L;
        Long memberId = 1L;
        Exception exception = new RuntimeException("분석 실패");
        
        InsightReport report = mock(InsightReport.class);
        when(report.getId()).thenReturn(reportId);
        when(report.getRequestedBy()).thenReturn(memberId);
        when(report.getStatus()).thenReturn(InsightReportStatus.PROCESSING);
        when(report.getRetryCount()).thenReturn((byte) 2);
        
        when(insightReportRepository.findById(reportId)).thenReturn(Optional.of(report));
        when(insightReportRepository.save(any(InsightReport.class))).thenReturn(report);
        doNothing().when(memberPointClient).refundPoints(eq(memberId), eq(80), anyString());
        
        // When
        service.handleAnalysisFailure(reportId, exception);
        
        // Then
        verify(insightReportRepository).save(any(InsightReport.class));
        verify(memberPointClient).refundPoints(eq(memberId), eq(80), eq("AI 분석 영구 실패로 인한 환불"));
    }

    // ========== Market 관련 테스트 ==========
    
    @Test
    @DisplayName("Market 리포트 생성 - 정상 요청 → PENDING 상태 리포트 생성")
    void requestMarketReport_validRequest_success() {
        // Given
        Long memberId = 1L;
        Long marketId = 100L;
        
        // 기존 리포트 없음
        when(insightReportRepository.findByTypeAndReferenceId(InsightReportType.MARKET, marketId))
                .thenReturn(Optional.empty());
        
        // 포인트 차감 성공
        doNothing().when(memberPointClient).spendPoints(eq(memberId), eq(80), anyString());
        
        // SETTLED Market
        MarketInsightSummaryResponse.MarketInfo marketInfo = MarketInsightSummaryResponse.MarketInfo.builder()
                .marketId(marketId)
                .title("Test Market")
                .status("SETTLED")
                .build();
        MarketInsightSummaryResponse marketResponse = MarketInsightSummaryResponse.builder()
                .market(marketInfo)
                .build();
        when(marketClient.getSummary(marketId)).thenReturn(marketResponse);
        
        // 새 리포트 저장 후 ID 설정을 모킹
        InsightReport savedReport = mock(InsightReport.class);
        when(savedReport.getId()).thenReturn(1L);
        when(savedReport.getStatus()).thenReturn(InsightReportStatus.PENDING);
        when(insightReportRepository.save(any(InsightReport.class))).thenReturn(savedReport);
        
        // When
        InsightReportResponse response = service.requestMarketReport(memberId, marketId, "test-key");
        
        // Then
        assertThat(response.getReportId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getPointCharged()).isEqualTo(80);
        assertThat(response.getReportContent()).isNull();
        
        verify(memberPointClient).spendPoints(eq(memberId), eq(80), anyString());
        verify(marketClient).getSummary(marketId);
        verify(insightReportRepository).save(any(InsightReport.class));
    }
    
    @Test
    @DisplayName("Market 리포트 생성 - 기존 DONE 리포트 존재 → 즉시 반환")
    void requestMarketReport_existingDoneReport_returnImmediately() {
        // Given
        Long memberId = 1L;
        Long marketId = 100L;
        
        InsightReport existingReport = mock(InsightReport.class);
        when(existingReport.getId()).thenReturn(1L);
        when(existingReport.getStatus()).thenReturn(InsightReportStatus.DONE);
        when(existingReport.getReportContent()).thenReturn("기존 분석 결과");
        when(existingReport.getGeneratedAt()).thenReturn(LocalDateTime.now());
        
        when(insightReportRepository.findByTypeAndReferenceId(InsightReportType.MARKET, marketId))
                .thenReturn(Optional.of(existingReport));
        
        // When
        InsightReportResponse response = service.requestMarketReport(memberId, marketId, "test-key");
        
        // Then
        assertThat(response.getReportId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo("DONE");
        assertThat(response.getPointCharged()).isEqualTo(0);  // 포인트 차감 없음
        assertThat(response.getReportContent()).isEqualTo("기존 분석 결과");
        
        verify(memberPointClient, never()).spendPoints(anyLong(), anyInt(), anyString());
        verify(marketClient, never()).getSummary(anyLong());
        verify(insightReportRepository, never()).save(any(InsightReport.class));
    }
    
    @Test
    @DisplayName("Market 리포트 생성 - PROCESSING 중복 요청 → ALREADY_PROCESSING 오류")
    void requestMarketReport_alreadyProcessing_throwsException() {
        // Given
        Long memberId = 1L;
        Long marketId = 100L;
        
        InsightReport processingReport = mock(InsightReport.class);
        when(processingReport.getId()).thenReturn(1L);
        when(processingReport.getStatus()).thenReturn(InsightReportStatus.PROCESSING);
        
        when(insightReportRepository.findByTypeAndReferenceId(InsightReportType.MARKET, marketId))
                .thenReturn(Optional.of(processingReport));
        
        // When & Then
        assertThatThrownBy(() -> service.requestMarketReport(memberId, marketId, "test-key"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INSIGHT_REPORT_ALREADY_PROCESSING);
        
        verify(memberPointClient, never()).spendPoints(anyLong(), anyInt(), anyString());
    }
    
    @Test
    @DisplayName("Market 리포트 생성 - Market 미정산 → SOURCE_DATA_NOT_READY 오류 + 환불")
    void requestMarketReport_marketNotSettled_throwsExceptionAndRefund() {
        // Given
        Long memberId = 1L;
        Long marketId = 100L;
        
        when(insightReportRepository.findByTypeAndReferenceId(InsightReportType.MARKET, marketId))
                .thenReturn(Optional.empty());
        
        // 포인트 차감 성공
        doNothing().when(memberPointClient).spendPoints(eq(memberId), eq(80), anyString());
        
        // Market 미정산 상태
        when(marketClient.getSummary(marketId))
                .thenThrow(new CustomException(ErrorCode.INSIGHT_REPORT_SOURCE_DATA_NOT_READY));
        
        // 환불 처리
        doNothing().when(memberPointClient).refundPoints(eq(memberId), eq(80), anyString());
        
        // When & Then
        assertThatThrownBy(() -> service.requestMarketReport(memberId, marketId, "test-key"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INSIGHT_REPORT_SOURCE_DATA_NOT_READY);
        
        verify(memberPointClient).spendPoints(eq(memberId), eq(80), anyString());
        verify(memberPointClient).refundPoints(eq(memberId), eq(80), eq("Market 미정산으로 인한 환불"));
        verify(insightReportRepository, never()).save(any(InsightReport.class));
    }
    
    @Test
    @DisplayName("Market 리포트 생성 - 포인트 부족 → POINT_INSUFFICIENT 오류")
    void requestMarketReport_insufficientPoints_throwsException() {
        // Given
        Long memberId = 1L;
        Long marketId = 100L;
        
        when(insightReportRepository.findByTypeAndReferenceId(InsightReportType.MARKET, marketId))
                .thenReturn(Optional.empty());
        
        doThrow(new CustomException(ErrorCode.POINT_INSUFFICIENT))
                .when(memberPointClient).spendPoints(eq(memberId), eq(80), anyString());
        
        // When & Then
        assertThatThrownBy(() -> service.requestMarketReport(memberId, marketId, "test-key"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POINT_INSUFFICIENT);
        
        verify(marketClient, never()).getSummary(anyLong());
        verify(insightReportRepository, never()).save(any(InsightReport.class));
    }
    
    @Test
    @DisplayName("Market 리포트 상태 조회 - 정상 조회")
    void getMarketReportStatus_validRequest_success() {
        // Given
        Long marketId = 100L;
        
        InsightReport report = mock(InsightReport.class);
        when(report.getId()).thenReturn(1L);
        when(report.getStatus()).thenReturn(InsightReportStatus.PROCESSING);
        when(report.getRetryCount()).thenReturn((byte) 1);
        when(report.getCreatedAt()).thenReturn(LocalDateTime.now().minusMinutes(5));
        when(report.getProcessingStartedAt()).thenReturn(LocalDateTime.now().minusMinutes(3));
        
        when(insightReportRepository.findByTypeAndReferenceId(InsightReportType.MARKET, marketId))
                .thenReturn(Optional.of(report));
        
        // When
        InsightReportStatusResponse response = service.getMarketReportStatus(marketId);
        
        // Then
        assertThat(response.getReportId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo("PROCESSING");
        assertThat(response.getRetryCount()).isEqualTo(1);
        assertThat(response.getProcessingStartedAt()).isNotNull();
    }
    
    @Test
    @DisplayName("Market 리포트 상태 조회 - 리포트 없음 → NOT_FOUND 오류")
    void getMarketReportStatus_reportNotFound_throwsException() {
        // Given
        Long marketId = 100L;
        
        when(insightReportRepository.findByTypeAndReferenceId(InsightReportType.MARKET, marketId))
                .thenReturn(Optional.empty());
        
        // When & Then
        assertThatThrownBy(() -> service.getMarketReportStatus(marketId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INSIGHT_REPORT_NOT_FOUND);
    }
}