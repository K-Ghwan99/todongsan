package com.todongsan.insightreputation.insight.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class InsightReportStatusResponse {
    
    private Long reportId;
    private String status;                      // "PENDING", "PROCESSING", "DONE", "FAILED"
    private Integer retryCount;                 // 재시도 횟수
    private LocalDateTime createdAt;            // 생성 시각
    private LocalDateTime processingStartedAt;  // 처리 시작 시각
    private LocalDateTime generatedAt;          // 완료 시각
}