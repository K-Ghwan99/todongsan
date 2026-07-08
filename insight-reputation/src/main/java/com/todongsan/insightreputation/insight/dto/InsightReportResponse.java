package com.todongsan.insightreputation.insight.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class InsightReportResponse {
    
    private Long reportId;
    private Long battleId;              // Battle ID (관리자 조회 시 포함)
    private String status;              // "PENDING", "PROCESSING", "DONE", "FAILED"
    private String title;               // Battle 제목 (관리자 조회 시 포함)
    private String summary;             // AI 분석 요약 (DONE 상태일 때만)
    private String reportContent;       // AI 분석 결과 (DONE 상태일 때만)
    private String analysisData;        // 분석에 사용된 원본 데이터 (관리자 조회 시 포함)
    private LocalDateTime generatedAt;  // 완료 시각 (DONE 상태일 때만)
    private Integer pointCharged;       // 차감된 포인트 (0: 기존 완료 리포트, 80: 신규 생성)
    private Integer retryCount;         // 재시도 횟수 (관리자 조회 시 포함)
    private String failedReason;        // 실패 사유 (FAILED 상태일 때만, 관리자 조회 시 포함)
}