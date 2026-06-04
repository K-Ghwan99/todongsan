package com.todongsan.battle_service.battle.service;

import com.todongsan.battle_service.battle.dto.request.BattleCreateRequest;
import com.todongsan.battle_service.battle.dto.response.BattleCreateResponse;
import com.todongsan.battle_service.battle.dto.response.BattleDetailResponse;
import com.todongsan.battle_service.battle.dto.response.BattleStatusResponse;
import com.todongsan.battle_service.battle.entity.Battle;
import com.todongsan.battle_service.battle.entity.BattleStatus;
import com.todongsan.battle_service.battle.repository.BattleRepository;
import com.todongsan.battle_service.global.exception.CustomException;
import com.todongsan.battle_service.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BattleServiceImplTest {

    @Mock
    private BattleRepository battleRepository;

    @InjectMocks
    private BattleServiceImpl battleService;

    // ===================== createBattle =====================

    @Test
    @DisplayName("Battle 생성 성공")
    void createBattle_success() {
        Long memberId = 1L;
        BattleCreateRequest request = BattleCreateRequest.builder()
                .title("성수 vs 연남")
                .optionA("성수")
                .optionB("연남")
                .startAt(LocalDateTime.now().plusDays(1))
                .endAt(LocalDateTime.now().plusDays(7))
                .build();

        Battle saved = Battle.builder()
                .title(request.getTitle())
                .optionA(request.getOptionA())
                .optionB(request.getOptionB())
                .createdBy(memberId)
                .startAt(request.getStartAt())
                .endAt(request.getEndAt())
                .build();
        ReflectionTestUtils.setField(saved, "id", 1L);

        given(battleRepository.save(any(Battle.class))).willReturn(saved);

        BattleCreateResponse response = battleService.createBattle(memberId, request);

        assertThat(response.getBattleId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("Battle 생성 실패 - endAt이 startAt 이전")
    void createBattle_fail_endAtBeforeStartAt() {
        BattleCreateRequest request = BattleCreateRequest.builder()
                .title("테스트")
                .optionA("A")
                .optionB("B")
                .startAt(LocalDateTime.now().plusDays(7))
                .endAt(LocalDateTime.now().plusDays(1))
                .build();

        assertThatThrownBy(() -> battleService.createBattle(1L, request))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.BATTLE_INVALID_PERIOD));
    }

    @Test
    @DisplayName("Battle 생성 실패 - endAt이 현재 시각 이전")
    void createBattle_fail_endAtInPast() {
        BattleCreateRequest request = BattleCreateRequest.builder()
                .title("테스트")
                .optionA("A")
                .optionB("B")
                .startAt(LocalDateTime.now().minusDays(10))
                .endAt(LocalDateTime.now().minusDays(1))
                .build();

        assertThatThrownBy(() -> battleService.createBattle(1L, request))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.BATTLE_INVALID_PERIOD));
    }

    // ===================== getBattles =====================

    @Test
    @DisplayName("Battle 목록 조회 성공 - ACTIVE")
    void getBattles_activeStatus() {
        Battle battle = activeBattle();
        Page<Battle> page = new PageImpl<>(List.of(battle));
        given(battleRepository.findByStatusAndDeletedAtIsNull(eq(BattleStatus.ACTIVE), any(Pageable.class)))
                .willReturn(page);

        var result = battleService.getBattles("ACTIVE", 0, 20);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Battle 목록 조회 실패 - PENDING 요청 시 VALIDATION_FAILED")
    void getBattles_fail_pendingStatus() {
        assertThatThrownBy(() -> battleService.getBattles("PENDING", 0, 20))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.VALIDATION_FAILED));
    }

    // ===================== getBattle =====================

    @Test
    @DisplayName("Battle 상세 조회 성공")
    void getBattle_success() {
        Battle battle = activeBattle();
        given(battleRepository.findByIdAndStatusInAndDeletedAtIsNull(eq(1L), any()))
                .willReturn(Optional.of(battle));

        BattleDetailResponse response = battleService.getBattle(1L);

        assertThat(response.getBattleId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Battle 상세 조회 실패 - 존재하지 않음")
    void getBattle_fail_notFound() {
        given(battleRepository.findByIdAndStatusInAndDeletedAtIsNull(any(), any()))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> battleService.getBattle(999L))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.BATTLE_NOT_FOUND));
    }

    // ===================== approveBattle =====================

    @Test
    @DisplayName("Battle 승인 성공 - PENDING → ACTIVE")
    void approveBattle_success() {
        Battle battle = pendingBattle();
        given(battleRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(battle));

        BattleStatusResponse response = battleService.approveBattle(1L);

        assertThat(response.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("Battle 승인 실패 - PENDING 아님")
    void approveBattle_fail_notPending() {
        Battle battle = activeBattle();
        given(battleRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(battle));

        assertThatThrownBy(() -> battleService.approveBattle(1L))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.BATTLE_INVALID_STATUS));
    }

    // ===================== rejectBattle =====================

    @Test
    @DisplayName("Battle 거절 성공 - PENDING → CANCELLED")
    void rejectBattle_success() {
        Battle battle = pendingBattle();
        given(battleRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(battle));

        BattleStatusResponse response = battleService.rejectBattle(1L);

        assertThat(response.getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    @DisplayName("Battle 거절 실패 - PENDING 아님")
    void rejectBattle_fail_notPending() {
        Battle battle = activeBattle();
        given(battleRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(battle));

        assertThatThrownBy(() -> battleService.rejectBattle(1L))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.BATTLE_INVALID_STATUS));
    }

    // ===================== cancelBattle =====================

    @Test
    @DisplayName("Battle 강제 취소 성공 - ACTIVE → CANCELLED")
    void cancelBattle_success() {
        Battle battle = activeBattle();
        given(battleRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(battle));

        BattleStatusResponse response = battleService.cancelBattle(1L);

        assertThat(response.getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    @DisplayName("Battle 강제 취소 실패 - ACTIVE 아님")
    void cancelBattle_fail_notActive() {
        Battle battle = pendingBattle();
        given(battleRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(battle));

        assertThatThrownBy(() -> battleService.cancelBattle(1L))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.BATTLE_INVALID_STATUS));
    }

    // ===================== helpers =====================

    private Battle pendingBattle() {
        Battle battle = Battle.builder()
                .title("성수 vs 연남")
                .optionA("성수")
                .optionB("연남")
                .createdBy(1L)
                .startAt(LocalDateTime.now().plusDays(1))
                .endAt(LocalDateTime.now().plusDays(7))
                .build();
        ReflectionTestUtils.setField(battle, "id", 1L);
        return battle;
    }

    private Battle activeBattle() {
        Battle battle = pendingBattle();
        battle.approve();
        return battle;
    }
}
