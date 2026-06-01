package com.todongsan.insightreputation.insight.service;

import com.todongsan.insightreputation.global.client.BattleClient;
import com.todongsan.insightreputation.global.client.BattleResponse;
import com.todongsan.insightreputation.global.client.BattleVote;
import com.todongsan.insightreputation.global.client.BattleVotesRawResponse;
import com.todongsan.insightreputation.global.client.ClaudeApiClient;
import com.todongsan.insightreputation.global.client.MarketClient;
import com.todongsan.insightreputation.global.client.MarketInsightSummaryResponse;
import com.todongsan.insightreputation.global.client.MarketPredictionResponse;
import com.todongsan.insightreputation.global.client.MemberInfoResponse;
import com.todongsan.insightreputation.global.client.MemberPointClient;
import com.todongsan.insightreputation.global.exception.CustomException;
import com.todongsan.insightreputation.global.exception.errorcode.ErrorCode;
import com.todongsan.insightreputation.insight.dto.InsightReportResponse;
import com.todongsan.insightreputation.insight.dto.InsightReportStatusResponse;
import com.todongsan.insightreputation.insight.entity.InsightReport;
import com.todongsan.insightreputation.enums.InsightReportStatus;
import com.todongsan.insightreputation.enums.InsightReportType;
import com.todongsan.insightreputation.insight.repository.InsightReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InsightReportService {

    private final InsightReportRepository insightReportRepository;
    private final BattleClient battleClient;
    private final MarketClient marketClient;
    private final MemberPointClient memberPointClient;
    private final ClaudeApiClient claudeApiClient;
    
    private static final int REPORT_COST = 80;

    /**
     * Battle AI 분석 리포트 생성 요청
     * 
     * @param memberId 요청 회원 ID
     * @param battleId Battle ID
     * @return 리포트 응답
     */
    @Transactional
    public InsightReportResponse requestBattleReport(Long memberId, Long battleId) {
        log.info("Battle 리포트 생성 요청: memberId={}, battleId={}", memberId, battleId);
        
        // 1. 기존 리포트 확인
        Optional<InsightReport> existingReport = insightReportRepository
                .findByTypeAndReferenceId(InsightReportType.BATTLE, battleId);
        
        if (existingReport.isPresent()) {
            InsightReport report = existingReport.get();
            
            // DONE 상태면 즉시 반환 (Point 차감 없음)
            if (report.getStatus() == InsightReportStatus.DONE) {
                log.info("기존 완료 리포트 반환: reportId={}, battleId={}", report.getId(), battleId);
                return InsightReportResponse.builder()
                        .reportId(report.getId())
                        .status(report.getStatus().name())
                        .reportContent(report.getReportContent())
                        .generatedAt(report.getGeneratedAt())
                        .pointCharged(0)
                        .build();
            }
            
            // PENDING 또는 PROCESSING 상태면 중복 처리 거부
            if (report.getStatus() == InsightReportStatus.PENDING || 
                report.getStatus() == InsightReportStatus.PROCESSING) {
                log.warn("이미 처리 중인 리포트: reportId={}, status={}, battleId={}", 
                        report.getId(), report.getStatus(), battleId);
                throw new CustomException(ErrorCode.INSIGHT_REPORT_ALREADY_PROCESSING);
            }
        }
        
        // 2. Battle 상태 확인 (종료된 Battle만 분석 가능)
        BattleResponse battleInfo = battleClient.getBattleInfo(battleId);
        
        if (!battleInfo.getIsClosed()) {
            log.warn("종료되지 않은 Battle 분석 요청: battleId={}, status={}", 
                    battleId, battleInfo.getStatus());
            throw new CustomException(ErrorCode.INSIGHT_REPORT_SOURCE_NOT_CLOSED);
        }
        
        // 3. Point 차감 (멱등성 키 사용)
        String idempotencyKey = generateIdempotencyKey(memberId, battleId, "BATTLE");
        
        try {
            memberPointClient.spendPoints(memberId, REPORT_COST, idempotencyKey);
            log.info("포인트 차감 성공: memberId={}, amount={}, battleId={}", 
                    memberId, REPORT_COST, battleId);
        } catch (CustomException e) {
            if (e.getErrorCode() == ErrorCode.POINT_INSUFFICIENT) {
                log.warn("포인트 부족으로 리포트 생성 실패: memberId={}, battleId={}", memberId, battleId);
                throw e;
            }
            log.error("포인트 차감 실패: memberId={}, battleId={}", memberId, battleId, e);
            throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR);
        }
        
        // 4. 리포트 레코드 생성 (PENDING 상태)
        InsightReport newReport = InsightReport.builder()
                .type(InsightReportType.BATTLE)
                .referenceId(battleId)
                .requestedBy(memberId)
                .status(InsightReportStatus.PENDING)
                .retryCount(0)
                .build();
        
        InsightReport savedReport = insightReportRepository.save(newReport);
        log.info("리포트 레코드 생성: reportId={}, battleId={}", savedReport.getId(), battleId);
        
        // 5. 비동기 분석 시작
        generateBattleReportAsync(savedReport.getId());
        
        return InsightReportResponse.builder()
                .reportId(savedReport.getId())
                .status(savedReport.getStatus().name())
                .reportContent(null)  // PENDING 상태에서는 null
                .generatedAt(null)    // PENDING 상태에서는 null
                .pointCharged(REPORT_COST)
                .build();
    }
    
    /**
     * 리포트 상태 조회
     * 
     * @param battleId Battle ID
     * @return 리포트 상태 응답
     */
    @Transactional(readOnly = true)
    public InsightReportStatusResponse getBattleReportStatus(Long battleId) {
        Optional<InsightReport> reportOpt = insightReportRepository
                .findByTypeAndReferenceId(InsightReportType.BATTLE, battleId);
        
        if (reportOpt.isEmpty()) {
            throw new CustomException(ErrorCode.INSIGHT_REPORT_NOT_FOUND);
        }
        
        InsightReport report = reportOpt.get();
        
        return InsightReportStatusResponse.builder()
                .reportId(report.getId())
                .status(report.getStatus().name())
                .retryCount((int) report.getRetryCount())
                .createdAt(report.getCreatedAt())
                .processingStartedAt(report.getProcessingStartedAt())
                .generatedAt(report.getGeneratedAt())
                .build();
    }
    
    /**
     * 비동기 Battle 리포트 생성
     * 
     * @param reportId 리포트 ID
     */
    @Async
    public void generateBattleReportAsync(Long reportId) {
        log.info("비동기 Battle 리포트 생성 시작: reportId={}", reportId);
        
        try {
            // 1. 리포트 상태를 PROCESSING으로 전이
            boolean transitioned = transitionToPROCESSING(reportId);
            if (!transitioned) {
                log.warn("PROCESSING 전이 실패 (이미 처리됨?): reportId={}", reportId);
                return;
            }
            
            // 2. Battle 데이터 수집 및 AI 분석 수행
            performBattleAnalysis(reportId);
            
        } catch (Exception e) {
            log.error("Battle 리포트 생성 중 오류: reportId={}", reportId, e);
            handleAnalysisFailure(reportId, e);
        }
    }
    
    /**
     * 리포트 상태를 PROCESSING으로 전이
     * 
     * @param reportId 리포트 ID
     * @return 전이 성공 여부
     */
    @Transactional
    public boolean transitionToPROCESSING(Long reportId) {
        Optional<InsightReport> reportOpt = insightReportRepository.findById(reportId);
        if (reportOpt.isEmpty()) {
            log.warn("리포트 없음: reportId={}", reportId);
            return false;
        }
        
        InsightReport report = reportOpt.get();
        
        // PENDING 상태에서만 PROCESSING으로 전이 가능
        if (report.getStatus() != InsightReportStatus.PENDING) {
            log.warn("PROCESSING 전이 불가능한 상태: reportId={}, currentStatus={}", 
                    reportId, report.getStatus());
            return false;
        }
        
        report.startProcessing();  // 상태를 PROCESSING으로 변경하고 processingStartedAt 설정
        insightReportRepository.save(report);
        
        log.info("리포트 상태 PROCESSING 전이 완료: reportId={}", reportId);
        return true;
    }
    
    /**
     * Battle 분석 수행
     * 
     * @param reportId 리포트 ID
     */
    private void performBattleAnalysis(Long reportId) {
        InsightReport report = insightReportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("리포트 없음: " + reportId));
        
        Long battleId = report.getReferenceId();
        
        try {
            // 1. Battle 정보 조회
            BattleResponse battleInfo = battleClient.getBattleInfo(battleId);
            
            // 2. 투표 원본 데이터 조회
            BattleVotesRawResponse votesData = battleClient.getBattleVotesRaw(battleId);
            
            if (votesData.getVotes().isEmpty()) {
                log.warn("투표 데이터 없음: battleId={}", battleId);
                throw new RuntimeException("투표 데이터 없음");
            }
            
            // 3. 회원 정보 조회 (AI 분석용)
            List<Long> memberIds = votesData.getVotes().stream()
                    .map(BattleVote::getMemberId)
                    .distinct()
                    .collect(Collectors.toList());
            
            List<MemberInfoResponse> memberInfo = memberPointClient.getBatchMemberInfo(memberIds);
            
            // 4. AI 분석 프롬프트 생성
            String prompt = claudeApiClient.createBattleAnalysisPrompt(
                    battleInfo.getTitle(),
                    battleInfo.getOptionA(),
                    battleInfo.getOptionB(),
                    votesData.getVotes(),
                    memberInfo
            );
            
            // 5. Claude API를 통한 분석 수행
            String analysisResult = claudeApiClient.analyzeBattle(prompt);
            
            // 6. 분석 완료 처리
            completeAnalysis(reportId, analysisResult);
            
        } catch (Exception e) {
            log.error("Battle 분석 수행 중 오류: reportId={}, battleId={}", reportId, battleId, e);
            throw e;  // handleAnalysisFailure에서 처리
        }
    }
    
    /**
     * 분석 완료 처리
     * 
     * @param reportId 리포트 ID
     * @param analysisResult 분석 결과
     */
    @Transactional
    public void completeAnalysis(Long reportId, String analysisResult) {
        InsightReport report = insightReportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("리포트 없음: " + reportId));
        
        report.complete(analysisResult);  // 상태를 DONE으로 변경하고 결과 저장
        insightReportRepository.save(report);
        
        log.info("Battle 분석 완료: reportId={}, resultLength={}", reportId, analysisResult.length());
    }
    
    /**
     * 분석 실패 처리
     * 
     * @param reportId 리포트 ID
     * @param exception 발생한 예외
     */
    @Transactional
    public void handleAnalysisFailure(Long reportId, Exception exception) {
        InsightReport report = insightReportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("리포트 없음: " + reportId));
        
        int newRetryCount = report.getRetryCount() + 1;
        
        if (newRetryCount >= InsightReport.MAX_RETRY_COUNT) {
            // 최대 재시도 횟수 초과 → 영구 FAILED
            report.failPermanently(exception.getMessage());
            
            // Point 환불 (비동기, 실패해도 비즈니스 연속성 유지)
            try {
                memberPointClient.refundPoints(
                    report.getRequestedBy(), 
                    REPORT_COST, 
                    "AI 분석 영구 실패로 인한 환불"
                );
            } catch (Exception refundException) {
                log.error("환불 처리 중 오류 (비즈니스 연속성 유지): reportId={}", reportId, refundException);
            }
            
            log.error("리포트 영구 실패: reportId={}, retryCount={}", reportId, newRetryCount);
        } else {
            // 재시도 가능 → PENDING으로 리셋
            report.resetForRetry();
            log.warn("리포트 재시도 대기: reportId={}, retryCount={}", reportId, newRetryCount);
        }
        
        insightReportRepository.save(report);
    }
    
    /**
     * Market AI 정보 요약 생성 요청
     * 
     * @param memberId 요청 회원 ID
     * @param marketId Market ID
     * @return 리포트 응답
     */
    @Transactional
    public InsightReportResponse requestMarketReport(Long memberId, Long marketId) {
        log.info("Market 리포트 생성 요청: memberId={}, marketId={}", memberId, marketId);
        
        // 1. 기존 리포트 확인
        Optional<InsightReport> existingReport = insightReportRepository
                .findByTypeAndReferenceId(InsightReportType.MARKET, marketId);
        
        if (existingReport.isPresent()) {
            InsightReport report = existingReport.get();
            
            // DONE 상태면 즉시 반환 (Point 차감 없음)
            if (report.getStatus() == InsightReportStatus.DONE) {
                log.info("기존 완료 리포트 반환: reportId={}, marketId={}", report.getId(), marketId);
                return InsightReportResponse.builder()
                        .reportId(report.getId())
                        .status(report.getStatus().name())
                        .reportContent(report.getReportContent())
                        .generatedAt(report.getGeneratedAt())
                        .pointCharged(0)
                        .build();
            }
            
            // PENDING 또는 PROCESSING 상태면 중복 처리 거부
            if (report.getStatus() == InsightReportStatus.PENDING || 
                report.getStatus() == InsightReportStatus.PROCESSING) {
                log.warn("이미 처리 중인 리포트: reportId={}, status={}, marketId={}", 
                        report.getId(), report.getStatus(), marketId);
                throw new CustomException(ErrorCode.INSIGHT_REPORT_ALREADY_PROCESSING);
            }
        }
        
        // 2. Point 차감 (멱등성 키 사용) - Market 상태 확인 전에 차감
        String idempotencyKey = generateIdempotencyKey(memberId, marketId, "MARKET");
        
        try {
            memberPointClient.spendPoints(memberId, REPORT_COST, idempotencyKey);
            log.info("포인트 차감 성공: memberId={}, amount={}, marketId={}", 
                    memberId, REPORT_COST, marketId);
        } catch (CustomException e) {
            if (e.getErrorCode() == ErrorCode.POINT_INSUFFICIENT) {
                log.warn("포인트 부족으로 리포트 생성 실패: memberId={}, marketId={}", memberId, marketId);
                throw e;
            }
            log.error("포인트 차감 실패: memberId={}, marketId={}", memberId, marketId, e);
            throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR);
        }
        
        // 3. Market 상태 확인 (SETTLED 여부) - Point 차감 후 확인
        try {
            MarketInsightSummaryResponse marketInfo = marketClient.getSummary(marketId);
            log.info("Market 상태 확인 완료: marketId={}, status={}", marketId, marketInfo.getMarket().getStatus());
        } catch (CustomException e) {
            if (e.getErrorCode() == ErrorCode.INSIGHT_REPORT_SOURCE_DATA_NOT_READY) {
                // Market 미정산 상태 - Point 환불 처리
                log.warn("Market 미정산으로 리포트 생성 불가, 환불 처리: memberId={}, marketId={}", memberId, marketId);
                try {
                    memberPointClient.refundPoints(memberId, REPORT_COST, "Market 미정산으로 인한 환불");
                } catch (Exception refundException) {
                    log.error("환불 처리 중 오류 (비즈니스 연속성 유지): memberId={}, marketId={}", 
                             memberId, marketId, refundException);
                }
                throw e;
            }
            // 다른 Market Service 에러도 환불 처리
            log.error("Market 정보 조회 실패, 환불 처리: memberId={}, marketId={}", memberId, marketId, e);
            try {
                memberPointClient.refundPoints(memberId, REPORT_COST, "Market Service 오류로 인한 환불");
            } catch (Exception refundException) {
                log.error("환불 처리 중 오류 (비즈니스 연속성 유지): memberId={}, marketId={}", 
                         memberId, marketId, refundException);
            }
            throw e;
        }
        
        // 4. 리포트 레코드 생성 (PENDING 상태)
        InsightReport newReport = InsightReport.builder()
                .type(InsightReportType.MARKET)
                .referenceId(marketId)
                .requestedBy(memberId)
                .status(InsightReportStatus.PENDING)
                .retryCount(0)
                .build();
        
        InsightReport savedReport = insightReportRepository.save(newReport);
        log.info("리포트 레코드 생성: reportId={}, marketId={}", savedReport.getId(), marketId);
        
        // 5. 비동기 분석 시작
        generateMarketReportAsync(savedReport.getId());
        
        return InsightReportResponse.builder()
                .reportId(savedReport.getId())
                .status(savedReport.getStatus().name())
                .reportContent(null)  // PENDING 상태에서는 null
                .generatedAt(null)    // PENDING 상태에서는 null
                .pointCharged(REPORT_COST)
                .build();
    }
    
    /**
     * Market 리포트 상태 조회
     * 
     * @param marketId Market ID
     * @return 리포트 상태 응답
     */
    @Transactional(readOnly = true)
    public InsightReportStatusResponse getMarketReportStatus(Long marketId) {
        Optional<InsightReport> reportOpt = insightReportRepository
                .findByTypeAndReferenceId(InsightReportType.MARKET, marketId);
        
        if (reportOpt.isEmpty()) {
            throw new CustomException(ErrorCode.INSIGHT_REPORT_NOT_FOUND);
        }
        
        InsightReport report = reportOpt.get();
        
        return InsightReportStatusResponse.builder()
                .reportId(report.getId())
                .status(report.getStatus().name())
                .retryCount((int) report.getRetryCount())
                .createdAt(report.getCreatedAt())
                .processingStartedAt(report.getProcessingStartedAt())
                .generatedAt(report.getGeneratedAt())
                .build();
    }
    
    /**
     * 비동기 Market 리포트 생성
     * 
     * @param reportId 리포트 ID
     */
    @Async
    public void generateMarketReportAsync(Long reportId) {
        log.info("비동기 Market 리포트 생성 시작: reportId={}", reportId);
        
        try {
            // 1. 리포트 상태를 PROCESSING으로 전이
            boolean transitioned = transitionToPROCESSING(reportId);
            if (!transitioned) {
                log.warn("PROCESSING 전이 실패 (이미 처리됨?): reportId={}", reportId);
                return;
            }
            
            // 2. Market 데이터 수집 및 AI 분석 수행
            performMarketAnalysis(reportId);
            
        } catch (Exception e) {
            log.error("Market 리포트 생성 중 오류: reportId={}", reportId, e);
            handleAnalysisFailure(reportId, e);
        }
    }
    
    /**
     * Market 분석 수행
     * 
     * @param reportId 리포트 ID
     */
    private void performMarketAnalysis(Long reportId) {
        InsightReport report = insightReportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("리포트 없음: " + reportId));
        
        Long marketId = report.getReferenceId();
        
        try {
            // 1. Market 정보 조회
            MarketInsightSummaryResponse marketInfo = marketClient.getSummary(marketId);
            
            // 2. 예측 원본 데이터 조회
            List<MarketPredictionResponse> predictions = marketClient.getPredictions(marketId);
            
            if (predictions.isEmpty()) {
                log.warn("예측 데이터 없음: marketId={}", marketId);
                throw new RuntimeException("예측 데이터 없음");
            }
            
            // 3. 회원 정보 조회 (AI 분석용)
            List<Long> memberIds = predictions.stream()
                    .map(MarketPredictionResponse::getMemberId)
                    .distinct()
                    .collect(Collectors.toList());
            
            List<MemberInfoResponse> memberInfo = memberPointClient.getBatchMemberInfo(memberIds);
            
            // 4. AI 분석 프롬프트 생성
            String prompt = claudeApiClient.createMarketAnalysisPrompt(
                    marketInfo.getMarket().getTitle(),
                    marketInfo.getOptionStatistics(),
                    predictions,
                    memberInfo
            );
            
            // 5. Claude API를 통한 분석 수행
            String analysisResult = claudeApiClient.analyzeBattle(prompt);
            
            // 6. 분석 완료 처리
            completeAnalysis(reportId, analysisResult);
            
        } catch (Exception e) {
            log.error("Market 분석 수행 중 오류: reportId={}, marketId={}", reportId, marketId, e);
            throw e;  // handleAnalysisFailure에서 처리
        }
    }

    /**
     * 멱등성 키 생성
     * 
     * @param memberId 회원 ID
     * @param referenceId 참조 ID (battleId 또는 marketId)
     * @param type 타입 구분자
     * @return 멱등성 키
     */
    private String generateIdempotencyKey(Long memberId, Long referenceId, String type) {
        return String.format("%s-report-%d-%d-%s", type.toLowerCase(), memberId, referenceId, 
                LocalDateTime.now().toLocalDate().toString());
    }
}