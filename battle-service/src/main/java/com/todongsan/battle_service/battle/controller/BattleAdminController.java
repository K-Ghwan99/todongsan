package com.todongsan.battle_service.battle.controller;

import com.todongsan.battle_service.battle.dto.response.BattleStatusResponse;
import com.todongsan.battle_service.battle.service.BattleService;
import com.todongsan.battle_service.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/battles")
@RequiredArgsConstructor
public class BattleAdminController {

    private final BattleService battleService;

    // PATCH /api/v1/battles/{battleId}/approve
    @PatchMapping("/{battleId}/approve")
    public ApiResponse<BattleStatusResponse> approveBattle(@PathVariable Long battleId) {
        return ApiResponse.ok(battleService.approveBattle(battleId));
    }

    // PATCH /api/v1/battles/{battleId}/reject
    @PatchMapping("/{battleId}/reject")
    public ApiResponse<BattleStatusResponse> rejectBattle(@PathVariable Long battleId) {
        return ApiResponse.ok(battleService.rejectBattle(battleId));
    }

    // PATCH /api/v1/battles/{battleId}/cancel
    @PatchMapping("/{battleId}/cancel")
    public ApiResponse<BattleStatusResponse> cancelBattle(@PathVariable Long battleId) {
        return ApiResponse.ok(battleService.cancelBattle(battleId));
    }
}
