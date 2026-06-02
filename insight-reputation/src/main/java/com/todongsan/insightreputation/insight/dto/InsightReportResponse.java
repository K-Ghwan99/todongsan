package com.todongsan.insightreputation.insight.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class InsightReportResponse {
    
    private Long reportId;
    private String status;              // "PENDING", "PROCESSING", "DONE", "FAILED"
    private String reportContent;       // AI 분석 결과 (DONE 상태일 때만)
    private LocalDateTime generatedAt;  // 완료 시각 (DONE 상태일 때만)
    private Integer pointCharged;       // 차감된 포인트 (0: 기존 완료 리포트, 80: 신규 생성)
}