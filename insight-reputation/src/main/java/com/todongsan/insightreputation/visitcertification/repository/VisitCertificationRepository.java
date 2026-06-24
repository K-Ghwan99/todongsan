package com.todongsan.insightreputation.visitcertification.repository;

import com.todongsan.insightreputation.enums.VisitCertMethod;
import com.todongsan.insightreputation.visitcertification.entity.VisitCertification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VisitCertificationRepository extends JpaRepository<VisitCertification, Long> {

    Optional<VisitCertification> findByMemberIdAndSidoAndSigu(Long memberId, String sido, String sigu);

    List<VisitCertification> findByMemberIdOrderByCertifiedAtDesc(Long memberId);

    boolean existsByMemberIdAndSidoAndSigu(Long memberId, String sido, String sigu);

    // 방문 인증 회원 ID 조회 (Battle analysisData 빌드용)
    @Query("SELECT vc.memberId FROM VisitCertification vc WHERE vc.sido = :sido AND vc.sigu = :sigu")
    List<Long> findMemberIdsBySidoAndSigu(@Param("sido") String sido, @Param("sigu") String sigu);

    @Query("SELECT vc.memberId FROM VisitCertification vc WHERE vc.sido = :sido")
    List<Long> findMemberIdsBySido(@Param("sido") String sido);

    // 플랫폼 KPI 대시보드용
    long countByMethod(VisitCertMethod method);

    // 전국 가격 지도용
    @Query("SELECT vc.sido, COUNT(vc) FROM VisitCertification vc GROUP BY vc.sido")
    List<Object[]> countGroupBySido();

    // 활동 시계열 추이용 (native — YEARWEEK는 MySQL 전용)
    @Query(value = """
            SELECT YEARWEEK(vc.certified_at, 1), COUNT(vc.id)
            FROM visit_certification vc
            WHERE vc.certified_at >= :from
            GROUP BY YEARWEEK(vc.certified_at, 1)
            ORDER BY YEARWEEK(vc.certified_at, 1)
            """, nativeQuery = true)
    List<Object[]> countNewCertsByWeek(@Param("from") LocalDateTime from);

    // Market 통합 대시보드용
    long countBySidoAndMethod(String sido, VisitCertMethod method);

    long countBySidoAndSiguAndMethod(String sido, String sigu, VisitCertMethod method);

    long countBySido(String sido);

    long countBySidoAndSigu(String sido, String sigu);
}