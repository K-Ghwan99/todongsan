package com.todongsan.battle_service.battle.service;

import com.todongsan.battle_service.battle.dto.request.BattleCreateRequest;
import com.todongsan.battle_service.battle.dto.response.*;
import org.springframework.data.domain.Page;

public interface BattleService {

    BattleCreateResponse createBattle(Long memberId, BattleCreateRequest request);

    Page<BattleListResponse> getBattles(String status, int page, int size);

    BattleDetailResponse getBattle(Long battleId);

    BattleStatusResponse approveBattle(Long battleId);

    BattleStatusResponse rejectBattle(Long battleId);

    BattleStatusResponse cancelBattle(Long battleId);

    // 내부 Insight API 전용 (PENDING/CANCELLED도 노출)
    BattleDetailResponse getBattleInternal(Long battleId);
}
