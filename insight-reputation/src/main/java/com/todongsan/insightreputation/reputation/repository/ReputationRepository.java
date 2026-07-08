package com.todongsan.insightreputation.reputation.repository;

import com.todongsan.insightreputation.reputation.entity.Reputation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReputationRepository extends JpaRepository<Reputation, Long> {

    Optional<Reputation> findByMemberId(Long memberId);

    boolean existsByMemberId(Long memberId);

    @Query("SELECT AVG(r.activityScore) FROM Reputation r")
    Double avgActivityScore();

    @Query("SELECT AVG(r.predictionAccuracy) FROM Reputation r WHERE r.predictionCount > 0")
    Double avgPredictionAccuracy();

    @Query(value = """
            SELECT
                CASE
                    WHEN activity_score BETWEEN 0  AND 20  THEN '0-20'
                    WHEN activity_score BETWEEN 21 AND 40  THEN '21-40'
                    WHEN activity_score BETWEEN 41 AND 60  THEN '41-60'
                    WHEN activity_score BETWEEN 61 AND 80  THEN '61-80'
                    ELSE '81-100'
                END AS bucket,
                COUNT(id)
            FROM reputation
            GROUP BY bucket
            """, nativeQuery = true)
    List<Object[]> countByActivityScoreBucket();
}