package com.todongsan.insightreputation.insight.repository;

import com.todongsan.insightreputation.enums.InsightReportStatus;
import com.todongsan.insightreputation.enums.InsightReportType;
import com.todongsan.insightreputation.insight.entity.InsightReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InsightReportRepository extends JpaRepository<InsightReport, Long> {
    
    Optional<InsightReport> findByTypeAndReferenceId(InsightReportType type, Long referenceId);
    
    List<InsightReport> findByStatus(InsightReportStatus status);
    
    @Query("SELECT ir FROM InsightReport ir WHERE ir.status = :status AND ir.processingStartedAt < :timeout")
    List<InsightReport> findTimeoutProcessingReports(@Param("status") InsightReportStatus status,
                                                      @Param("timeout") LocalDateTime timeout);
    
    List<InsightReport> findByStatusAndRetryCountLessThan(InsightReportStatus status, Byte maxRetryCount);

    @Query("SELECT ir FROM InsightReport ir WHERE ir.status = :status AND ir.createdAt < :cutoff")
    List<InsightReport> findOrphanedPendingReports(@Param("status") InsightReportStatus status,
                                                    @Param("cutoff") LocalDateTime cutoff);
}