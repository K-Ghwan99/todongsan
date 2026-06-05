package com.todongsan.battle_service.battle.controller;

import com.todongsan.battle_service.battle.dto.response.BattleStatusResponse;
import com.todongsan.battle_service.battle.service.BattleService;
import com.todongsan.battle_service.global.exception.CustomException;
import com.todongsan.battle_service.global.exception.ErrorCode;
import com.todongsan.battle_service.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/battles")
@RequiredArgsConstructor
public class BattleAdminController {

    private final BattleService battleService;

    @PatchMapping("/{battleId}/approve")
    public ApiResponse<BattleStatusResponse> approveBattle(
            @PathVariable Long battleId,
            @RequestHeader("X-Member-Role") String role) {
        validateAdmin(role);
        return ApiResponse.ok(battleService.approveBattle(battleId));
    }

    @PatchMapping("/{battleId}/reject")
    public ApiResponse<BattleStatusResponse> rejectBattle(
            @PathVariable Long battleId,
            @RequestHeader("X-Member-Role") String role) {
        validateAdmin(role);
        return ApiResponse.ok(battleService.rejectBattle(battleId));
    }

    @PatchMapping("/{battleId}/cancel")
    public ApiResponse<BattleStatusResponse> cancelBattle(
            @PathVariable Long battleId,
            @RequestHeader("X-Member-Role") String role) {
        validateAdmin(role);
        return ApiResponse.ok(battleService.cancelBattle(battleId));
    }

    private void validateAdmin(String role) {
        if (!"ADMIN".equals(role)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }
}
