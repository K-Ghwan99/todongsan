package com.todongsan.battle_service.vote.repository;

import com.todongsan.battle_service.battle.entity.BattleStatus;
import com.todongsan.battle_service.vote.entity.BattleVote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BattleVoteRepository extends JpaRepository<BattleVote, Long> {

    Optional<BattleVote> findByBattleIdAndMemberId(Long battleId, Long memberId);

    List<BattleVote> findByBattleId(Long battleId);

    // 정산 배치: 특정 Battle의 승자 진영 중 미보상 투표자
    List<BattleVote> findByBattleIdAndSelectedOptionAndIsRewardedFalse(Long battleId, String selectedOption);

    boolean existsByBattleIdAndMemberId(Long battleId, Long memberId);

    // 마이페이지: 내가 투표한 배틀 목록 (battle_vote ⋈ battle, 최신 투표순)
    @Query(value = "SELECT b, v FROM BattleVote v, Battle b " +
            "WHERE v.battleId = b.id AND v.memberId = :memberId " +
            "AND b.status IN :statuses AND b.deletedAt IS NULL " +
            "ORDER BY v.createdAt DESC",
            countQuery = "SELECT COUNT(v) FROM BattleVote v, Battle b " +
                    "WHERE v.battleId = b.id AND v.memberId = :memberId " +
                    "AND b.status IN :statuses AND b.deletedAt IS NULL")
    Page<Object[]> findMyVotedBattles(@Param("memberId") Long memberId,
                                      @Param("statuses") List<BattleStatus> statuses,
                                      Pageable pageable);
}
