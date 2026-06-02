package com.todongsan.battle_service.vote.repository;

import com.todongsan.battle_service.vote.entity.BattleVote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BattleVoteRepository extends JpaRepository<BattleVote, Long> {

    Optional<BattleVote> findByBattleIdAndMemberId(Long battleId, Long memberId);

    List<BattleVote> findByBattleId(Long battleId);

    // 정산 배치: 특정 Battle의 승자 진영 중 미보상 투표자
    List<BattleVote> findByBattleIdAndSelectedOptionAndIsRewardedFalse(Long battleId, String selectedOption);

    boolean existsByBattleIdAndMemberId(Long battleId, Long memberId);
}
