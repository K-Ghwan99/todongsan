package com.todongsan.insightreputation.reputation.repository;

import com.todongsan.insightreputation.reputation.entity.MarketPredictionResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MarketPredictionResultRepository extends JpaRepository<MarketPredictionResult, Long> {

    /**
     * 회원 ID와 Market ID로 기존 처리 결과 조회 (멱등성 체크용)
     * 
     * @param memberId 회원 ID
     * @param marketId Market ID
     * @return 기존 처리 결과 (존재하지 않으면 Optional.empty())
     */
    Optional<MarketPredictionResult> findByMemberIdAndMarketId(Long memberId, Long marketId);

    /**
     * 회원 ID와 Market ID 조합의 존재 여부 확인
     * 
     * @param memberId 회원 ID
     * @param marketId Market ID
     * @return 존재 여부
     */
    boolean existsByMemberIdAndMarketId(Long memberId, Long marketId);
}