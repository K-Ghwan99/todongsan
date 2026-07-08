package com.todongsan.insightreputation.reputation.repository;

import com.todongsan.insightreputation.reputation.entity.MarketPredictionResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MarketPredictionResultRepository extends JpaRepository<MarketPredictionResult, Long> {

    Optional<MarketPredictionResult> findByMemberIdAndMarketId(Long memberId, Long marketId);

    boolean existsByMemberIdAndMarketId(Long memberId, Long marketId);

    @Query(value = """
            SELECT AVG(CASE WHEN is_correct = true THEN 1.0 ELSE 0.0 END) * 100
            FROM market_prediction_result
            """, nativeQuery = true)
    Double calculateCrowdIntelligenceScore();

    // 주간 예측 결과 처리 건수 (native — YEARWEEK는 MySQL 전용)
    @Query(value = """
            SELECT YEARWEEK(mpr.processed_at, 1), COUNT(mpr.id)
            FROM market_prediction_result mpr
            WHERE mpr.processed_at >= :from
            GROUP BY YEARWEEK(mpr.processed_at, 1)
            ORDER BY YEARWEEK(mpr.processed_at, 1)
            """, nativeQuery = true)
    List<Object[]> countByWeek(@Param("from") LocalDateTime from);
}