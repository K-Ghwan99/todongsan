package com.todongsan.insightreputation.insight.entity;

import com.todongsan.insightreputation.enums.InsightReportStatus;
import com.todongsan.insightreputation.enums.InsightReportType;
import com.todongsan.insightreputation.global.config.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI 분석 리포트 상태 관리
 * 
 * 상태 전이: PENDING → PROCESSING → DONE / FAILED
 * 역방향 전이 불가. PROCESSING 전이 시 processingStartedAt 기록.
 * retry_count >= 3: 영구 FAILED. 스케줄러 재시도 없음.
 */
@Entity
@Table(
    name = "insight_report",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_type_reference", 
        columnNames = {"type", "reference_id"}
    ),
    indexes = {
        @Index(name = "idx_type", columnList = "type"),
        @Index(name = "idx_status", columnList = "status")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InsightReport extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private InsightReportType type;

    @Column(name = "reference_id", nullable = false)
    private Long referenceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InsightReportStatus status = InsightReportStatus.PENDING;

    @Lob
    @Column(name = "summary")
    private String summary;

    @Column(name = "analysis_data", columnDefinition = "JSON")
    private String analysisData;

    @Lob
    @Column(name = "raw_prompt")
    private String rawPrompt;

    @Lob
    @Column(name = "failed_reason")
    private String failedReason;

    @Column(name = "processing_started_at")
    private LocalDateTime processingStartedAt;

    @Column(name = "retry_count", nullable = false)
    private Byte retryCount = 0;

    @Column(name = "requested_by", nullable = false)
    private Long requestedBy;

    @Lob
    @Column(name = "report_content")
    private String reportContent;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    public static final int MAX_RETRY_COUNT = 3;

    @Builder
    public InsightReport(InsightReportType type, Long referenceId, Long requestedBy, InsightReportStatus status, Integer retryCount) {
        this.type = type;
        this.referenceId = referenceId;
        this.requestedBy = requestedBy;
        this.status = status != null ? status : InsightReportStatus.PENDING;
        this.retryCount = retryCount != null ? retryCount.byteValue() : 0;
    }

    public void startProcessing() {
        this.status = InsightReportStatus.PROCESSING;
        this.processingStartedAt = LocalDateTime.now();
    }

    public void complete(String reportContent) {
        this.status = InsightReportStatus.DONE;
        this.reportContent = reportContent;
        this.generatedAt = LocalDateTime.now();
    }

    public void failPermanently(String reason) {
        this.status = InsightReportStatus.FAILED;
        this.failedReason = reason;
    }

    public void resetForRetry() {
        this.status = InsightReportStatus.PENDING;
        this.processingStartedAt = null;
        this.retryCount++;
    }

    public boolean canRetry() {
        return this.retryCount < MAX_RETRY_COUNT;
    }
}