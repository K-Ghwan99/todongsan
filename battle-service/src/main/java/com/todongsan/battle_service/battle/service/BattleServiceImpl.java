package com.todongsan.battle_service.battle.service;

import com.todongsan.battle_service.battle.dto.request.BattleCreateRequest;
import com.todongsan.battle_service.battle.dto.response.*;
import com.todongsan.battle_service.battle.entity.Battle;
import com.todongsan.battle_service.battle.entity.BattleStatus;
import com.todongsan.battle_service.battle.repository.BattleRepository;
import com.todongsan.battle_service.global.exception.CustomException;
import com.todongsan.battle_service.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BattleServiceImpl implements BattleService {

    private final BattleRepository battleRepository;

    @Override
    @Transactional
    public BattleCreateResponse createBattle(Long memberId, BattleCreateRequest request) {
        validatePeriod(request.getStartAt(), request.getEndAt());

        // TODO: Member-Point SPEND_BATTLE_CREATE 30P 차감 (Feature 5)

        Battle battle = Battle.builder()
                .title(request.getTitle())
                .optionA(request.getOptionA())
                .optionB(request.getOptionB())
                .createdBy(memberId)
                .startAt(request.getStartAt())
                .endAt(request.getEndAt())
                .build();

        return BattleCreateResponse.from(battleRepository.save(battle));
    }

    @Override
    public Page<BattleListResponse> getBattles(String status, int page, int size) {
        BattleStatus battleStatus = parsePublicStatus(status);
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return battleRepository.findByStatusAndDeletedAtIsNull(battleStatus, pageable)
                .map(BattleListResponse::from);
    }

    @Override
    public BattleDetailResponse getBattle(Long battleId) {
        Battle battle = battleRepository
                .findByIdAndStatusInAndDeletedAtIsNull(battleId,
                        List.of(BattleStatus.ACTIVE, BattleStatus.CLOSED))
                .orElseThrow(() -> new CustomException(ErrorCode.BATTLE_NOT_FOUND));
        return BattleDetailResponse.from(battle);
    }

    @Override
    @Transactional
    public BattleStatusResponse approveBattle(Long battleId) {
        Battle battle = findByIdOrThrow(battleId);
        if (battle.getStatus() != BattleStatus.PENDING) {
            throw new CustomException(ErrorCode.BATTLE_INVALID_STATUS);
        }
        battle.approve();

        // TODO: 생성자에게 EARN_BATTLE_APPROVED 20P 지급 (Feature 5)

        return BattleStatusResponse.from(battle);
    }

    @Override
    @Transactional
    public BattleStatusResponse rejectBattle(Long battleId) {
        Battle battle = findByIdOrThrow(battleId);
        if (battle.getStatus() != BattleStatus.PENDING) {
            throw new CustomException(ErrorCode.BATTLE_INVALID_STATUS);
        }
        battle.reject();
        return BattleStatusResponse.from(battle);
    }

    @Override
    @Transactional
    public BattleStatusResponse cancelBattle(Long battleId) {
        Battle battle = findByIdOrThrow(battleId);
        if (battle.getStatus() != BattleStatus.ACTIVE) {
            throw new CustomException(ErrorCode.BATTLE_INVALID_STATUS);
        }
        battle.cancel();
        return BattleStatusResponse.from(battle);
    }

    @Override
    public BattleDetailResponse getBattleInternal(Long battleId) {
        Battle battle = findByIdOrThrow(battleId);
        return BattleDetailResponse.from(battle);
    }

    private Battle findByIdOrThrow(Long battleId) {
        return battleRepository.findByIdAndDeletedAtIsNull(battleId)
                .orElseThrow(() -> new CustomException(ErrorCode.BATTLE_NOT_FOUND));
    }

    private void validatePeriod(LocalDateTime startAt, LocalDateTime endAt) {
        if (endAt.isBefore(startAt) || endAt.isEqual(startAt)) {
            throw new CustomException(ErrorCode.BATTLE_INVALID_PERIOD);
        }
        if (endAt.isBefore(LocalDateTime.now())) {
            throw new CustomException(ErrorCode.BATTLE_INVALID_PERIOD);
        }
    }

    private BattleStatus parsePublicStatus(String status) {
        if (status == null || status.equalsIgnoreCase("ACTIVE")) return BattleStatus.ACTIVE;
        if (status.equalsIgnoreCase("CLOSED")) return BattleStatus.CLOSED;
        throw new CustomException(ErrorCode.VALIDATION_FAILED);
    }
}
