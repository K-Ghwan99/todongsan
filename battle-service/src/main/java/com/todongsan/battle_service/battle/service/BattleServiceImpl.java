package com.todongsan.battle_service.battle.service;

import com.todongsan.battle_service.battle.dto.request.BattleCreateRequest;
import com.todongsan.battle_service.battle.dto.response.*;
import com.todongsan.battle_service.battle.entity.Battle;
import com.todongsan.battle_service.battle.entity.BattleStatus;
import com.todongsan.battle_service.battle.repository.BattleRepository;
import com.todongsan.battle_service.client.MemberPointClient;
import com.todongsan.battle_service.client.dto.PointEarnRequest;
import com.todongsan.battle_service.global.exception.CustomException;
import com.todongsan.battle_service.global.exception.ErrorCode;
import com.todongsan.battle_service.retry.entity.PointRewardRetryQueue;
import com.todongsan.battle_service.retry.repository.PointRewardRetryQueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BattleServiceImpl implements BattleService {

    private static final BigDecimal APPROVED_REWARD = BigDecimal.valueOf(20);

    private final BattleRepository battleRepository;
    private final MemberPointClient memberPointClient;
    private final PointRewardRetryQueueRepository retryQueueRepository;

    @Override
    @Transactional
    public BattleCreateResponse createBattle(Long memberId, BattleCreateRequest request) {
        validatePeriod(request.getStartAt(), request.getEndAt());

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

        String idempotencyKey = "battle:approved:" + battleId + ":member:" + battle.getCreatedBy();
        try {
            memberPointClient.earnPoint(PointEarnRequest.builder()
                    .memberId(battle.getCreatedBy())
                    .type("EARN_BATTLE_APPROVED")
                    .referenceType("BATTLE")
                    .referenceId(battleId)
                    .amount(APPROVED_REWARD)
                    .idempotencyKey(idempotencyKey)
                    .build());
        } catch (CustomException e) {
            if (e.getErrorCode() == ErrorCode.EXTERNAL_SERVICE_TIMEOUT) {
                if (!retryQueueRepository.existsByIdempotencyKey(idempotencyKey)) {
                    retryQueueRepository.save(PointRewardRetryQueue.builder()
                            .memberId(battle.getCreatedBy())
                            .referenceType("BATTLE")
                            .referenceId(battleId)
                            .type("EARN_BATTLE_APPROVED")
                            .amount(APPROVED_REWARD)
                            .idempotencyKey(idempotencyKey)
                            .build());
                }
                log.warn("Approved reward enqueued for retry: member={}, battle={}", battle.getCreatedBy(), battleId);
            } else {
                log.warn("Approved reward failed (4xx), manual correction needed: member={}, battle={}", battle.getCreatedBy(), battleId);
            }
        }

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

    @Override
    @Transactional
    public List<Long> closeExpiredBattles() {
        List<Battle> expired = battleRepository.findExpiredActiveBattles(LocalDateTime.now());
        List<Long> closedIds = new ArrayList<>();
        for (Battle battle : expired) {
            String winningOption = determineWinner(battle);
            battle.close(winningOption);
            closedIds.add(battle.getId());
            log.info("Battle [{}] closed. winner={}", battle.getId(), winningOption);
        }
        return closedIds;
    }

    private String determineWinner(Battle battle) {
        if (battle.getOptionACount() > battle.getOptionBCount()) return "A";
        if (battle.getOptionBCount() > battle.getOptionACount()) return "B";
        return "DRAW";
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
