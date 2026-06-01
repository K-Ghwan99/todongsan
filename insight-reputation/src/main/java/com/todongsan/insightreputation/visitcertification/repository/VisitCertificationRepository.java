package com.todongsan.insightreputation.visitcertification.repository;

import com.todongsan.insightreputation.visitcertification.entity.VisitCertification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VisitCertificationRepository extends JpaRepository<VisitCertification, Long> {
    
    Optional<VisitCertification> findByMemberIdAndSidoAndSigu(Long memberId, String sido, String sigu);
    
    List<VisitCertification> findByMemberIdOrderByCertifiedAtDesc(Long memberId);
    
    boolean existsByMemberIdAndSidoAndSigu(Long memberId, String sido, String sigu);
}