package com.todongsan.battle_service.battle.service;

import com.todongsan.battle_service.battle.dto.request.BattleCreateRequest;
import com.todongsan.battle_service.battle.dto.response.*;
import org.springframework.data.domain.Page;

import java.util.List;

public interface BattleService {

    BattleCreateResponse createBattle(Long memberId, BattleCreateRequest request);

    Page<BattleListResponse> getBattles(String status, int page, int size);

    // 마이페이지: 내가 만든 배틀 목록 (본인은 모든 상태 노출)
    Page<MyCreatedBattleResponse> getMyCreatedBattles(Long memberId, String status, int page, int size);

    BattleDetailResponse getBattle(Long battleId);

    BattleStatusResponse approveBattle(Long battleId);

    BattleStatusResponse rejectBattle(Long battleId);

    BattleStatusResponse cancelBattle(Long battleId);

    // 내부 Insight API 전용 (PENDING/CANCELLED도 노출)
    BattleDetailResponse getBattleInternal(Long battleId);

    // 스케줄러 전용 — CLOSED 처리 후 닫힌 battleId 목록 반환
    List<Long> closeExpiredBattles();
}
