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

    @Builder
    public InsightReport(InsightReportType type, Long referenceId) {
        this.type = type;
        this.referenceId = referenceId;
        this.status = InsightReportStatus.PENDING;
        this.retryCount = 0;
    }

    public void startProcessing() {
        this.status = InsightReportStatus.PROCESSING;
        this.processingStartedAt = LocalDateTime.now();
    }

    public void completeProcessing(String summary, String analysisData, String rawPrompt) {
        this.status = InsightReportStatus.DONE;
        this.summary = summary;
        this.analysisData = analysisData;
        this.rawPrompt = rawPrompt;
    }

    public void failProcessing(String reason) {
        this.status = InsightReportStatus.FAILED;
        this.failedReason = reason;
    }

    public void resetToPending() {
        this.status = InsightReportStatus.PENDING;
        this.processingStartedAt = null;
        this.retryCount++;
    }

    public boolean canRetry() {
        return this.retryCount < 3;
    }
}