package com.todongsan.insightreputation.reputation.repository;

import com.todongsan.insightreputation.reputation.entity.Reputation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReputationRepository extends JpaRepository<Reputation, Long> {
    
    Optional<Reputation> findByMemberId(Long memberId);
    
    boolean existsByMemberId(Long memberId);
}