package com.todongsan.battle_service.battle.controller;

import com.todongsan.battle_service.battle.dto.request.BattleCreateRequest;
import com.todongsan.battle_service.battle.dto.response.BattleCreateResponse;
import com.todongsan.battle_service.battle.dto.response.BattleDetailResponse;
import com.todongsan.battle_service.battle.dto.response.BattleListResponse;
import com.todongsan.battle_service.battle.dto.response.MyCreatedBattleResponse;
import com.todongsan.battle_service.battle.service.BattleService;
import com.todongsan.battle_service.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/battles")
@RequiredArgsConstructor
public class BattleController {

    private final BattleService battleService;

    // POST /api/v1/battles
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<BattleCreateResponse> createBattle(
            @RequestHeader("X-Member-Id") Long memberId,
            @Valid @RequestBody BattleCreateRequest request) {
        return ApiResponse.ok(battleService.createBattle(memberId, request));
    }

    // GET /api/v1/battles?status=ACTIVE&page=0&size=20
    @GetMapping
    public ApiResponse<Page<BattleListResponse>> getBattles(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(battleService.getBattles(status, page, size));
    }

    // GET /api/v1/battles/created/me?status=PENDING,ACTIVE&page=0&size=20
    @GetMapping("/created/me")
    public ApiResponse<Page<MyCreatedBattleResponse>> getMyCreatedBattles(
            @RequestHeader("X-Member-Id") Long memberId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(battleService.getMyCreatedBattles(memberId, status, page, size));
    }

    // GET /api/v1/battles/{battleId}
    @GetMapping("/{battleId}")
    public ApiResponse<BattleDetailResponse> getBattle(@PathVariable Long battleId) {
        return ApiResponse.ok(battleService.getBattle(battleId));
    }
}
