package com.todongsan.battle_service.battle.repository;

import com.todongsan.battle_service.battle.entity.Battle;
import com.todongsan.battle_service.battle.entity.BattleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BattleRepository extends JpaRepository<Battle, Long> {

    // 일반 사용자 단건 조회 (ACTIVE/CLOSED만, soft delete 제외)
    Optional<Battle> findByIdAndStatusInAndDeletedAtIsNull(Long id, List<BattleStatus> statuses);

    // 관리자·내부용 단건 조회 (모든 상태, soft delete 제외)
    Optional<Battle> findByIdAndDeletedAtIsNull(Long id);

    // 목록 조회
    Page<Battle> findByStatusAndDeletedAtIsNull(BattleStatus status, Pageable pageable);

    // 마이페이지: 내가 만든 배틀 목록 (본인은 모든 상태 노출, soft delete 제외)
    Page<Battle> findByCreatedByAndStatusInAndDeletedAtIsNull(Long createdBy, List<BattleStatus> statuses, Pageable pageable);

    // 마감 배치: end_at 도달한 ACTIVE Battle
    @Query("SELECT b FROM Battle b WHERE b.status = 'ACTIVE' AND b.endAt <= :now AND b.deletedAt IS NULL")
    List<Battle> findExpiredActiveBattles(LocalDateTime now);

    // 정산 배치: CLOSED이고 아직 settled_at 없는 Battle
    @Query("SELECT b FROM Battle b WHERE b.status = 'CLOSED' AND b.settledAt IS NULL AND b.deletedAt IS NULL")
    List<Battle> findUnsettledClosedBattles();

    // 상태별 카운트 (Insight Service 내부 API용)
    long countByStatusAndDeletedAtIsNull(BattleStatus status);

    // 투표 집계 원자적 UPDATE (같은 트랜잭션에서 INSERT 후 호출)
    @Modifying
    @Query(value = "UPDATE battle SET option_a_count = option_a_count + 1, vote_count = vote_count + 1 WHERE id = :battleId", nativeQuery = true)
    void incrementOptionA(Long battleId);

    @Modifying
    @Query(value = "UPDATE battle SET option_b_count = option_b_count + 1, vote_count = vote_count + 1 WHERE id = :battleId", nativeQuery = true)
    void incrementOptionB(Long battleId);
}
