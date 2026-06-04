package com.todongsan.battle_service.vote.service;

import com.todongsan.battle_service.battle.entity.Battle;
import com.todongsan.battle_service.battle.entity.BattleStatus;
import com.todongsan.battle_service.battle.repository.BattleRepository;
import com.todongsan.battle_service.global.exception.CustomException;
import com.todongsan.battle_service.global.exception.ErrorCode;
import com.todongsan.battle_service.vote.dto.request.VoteRequest;
import com.todongsan.battle_service.vote.dto.response.VoteResponse;
import com.todongsan.battle_service.vote.dto.response.VoteResultResponse;
import com.todongsan.battle_service.vote.dto.response.VoteRawResponse;
import com.todongsan.battle_service.vote.entity.BattleVote;
import com.todongsan.battle_service.vote.repository.BattleVoteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VoteServiceImplTest {

    @Mock
    private BattleRepository battleRepository;

    @Mock
    private BattleVoteRepository battleVoteRepository;

    @InjectMocks
    private VoteServiceImpl voteService;

    // ===================== vote =====================

    @Test
    @DisplayName("투표 성공 - 옵션 A")
    void vote_success_optionA() {
        Battle battle = activeBattle(
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(7)
        );
        given(battleRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(battle));
        given(battleVoteRepository.save(any(BattleVote.class))).willReturn(new BattleVote());

        VoteResponse response = voteService.vote(1L, 1L, voteRequest("A"));

        assertThat(response.getSelectedOption()).isEqualTo("A");
        verify(battleRepository).incrementOptionA(1L);
    }

    @Test
    @DisplayName("투표 성공 - 옵션 B")
    void vote_success_optionB() {
        Battle battle = activeBattle(
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(7)
        );
        given(battleRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(battle));
        given(battleVoteRepository.save(any(BattleVote.class))).willReturn(new BattleVote());

        VoteResponse response = voteService.vote(1L, 1L, voteRequest("B"));

        assertThat(response.getSelectedOption()).isEqualTo("B");
        verify(battleRepository).incrementOptionB(1L);
    }

    @Test
    @DisplayName("투표 실패 - PENDING 상태 Battle")
    void vote_fail_pendingBattle() {
        Battle battle = pendingBattle();
        given(battleRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(battle));

        assertThatThrownBy(() -> voteService.vote(1L, 1L, voteRequest("A")))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.BATTLE_NOT_FOUND));
    }

    @Test
    @DisplayName("투표 실패 - CLOSED 상태 Battle")
    void vote_fail_closedBattle() {
        Battle battle = closedBattle();
        given(battleRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(battle));

        assertThatThrownBy(() -> voteService.vote(1L, 1L, voteRequest("A")))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.BATTLE_CLOSED));
    }

    @Test
    @DisplayName("투표 실패 - CANCELLED 상태 Battle")
    void vote_fail_cancelledBattle() {
        Battle battle = cancelledBattle();
        given(battleRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(battle));

        assertThatThrownBy(() -> voteService.vote(1L, 1L, voteRequest("A")))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.BATTLE_CLOSED));
    }

    @Test
    @DisplayName("투표 실패 - start_at 미도달")
    void vote_fail_beforeStartAt() {
        Battle battle = activeBattle(
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(7)
        );
        given(battleRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(battle));

        assertThatThrownBy(() -> voteService.vote(1L, 1L, voteRequest("A")))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.BATTLE_CLOSED));
    }

    @Test
    @DisplayName("투표 실패 - 잘못된 옵션")
    void vote_fail_invalidOption() {
        Battle battle = activeBattle(
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(7)
        );
        given(battleRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(battle));

        assertThatThrownBy(() -> voteService.vote(1L, 1L, voteRequest("C")))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.BATTLE_INVALID_OPTION));
    }

    // ===================== getResult =====================

    @Test
    @DisplayName("결과 조회 - ACTIVE 배틀, 미투표 → 비공개")
    void getResult_active_notVoted_hidden() {
        Battle battle = activeBattle(
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(7)
        );
        given(battleRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(battle));
        given(battleVoteRepository.existsByBattleIdAndMemberId(1L, 1L)).willReturn(false);

        VoteResultResponse response = voteService.getResult(1L, 1L);

        assertThat(response.isResultVisible()).isFalse();
    }

    @Test
    @DisplayName("결과 조회 - ACTIVE 배틀, 투표 완료 → 공개")
    void getResult_active_voted_visible() {
        Battle battle = activeBattle(
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(7)
        );
        given(battleRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(battle));
        given(battleVoteRepository.existsByBattleIdAndMemberId(1L, 1L)).willReturn(true);

        VoteResultResponse response = voteService.getResult(1L, 1L);

        assertThat(response.isResultVisible()).isTrue();
    }

    @Test
    @DisplayName("결과 조회 - CLOSED 배틀, 72시간 미경과, 미투표 → 비공개")
    void getResult_closed_before72h_notVoted_hidden() {
        Battle battle = closedBattleWithEndAt(LocalDateTime.now().minusHours(1));
        given(battleRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(battle));
        given(battleVoteRepository.existsByBattleIdAndMemberId(1L, 1L)).willReturn(false);

        VoteResultResponse response = voteService.getResult(1L, 1L);

        assertThat(response.isResultVisible()).isFalse();
    }

    @Test
    @DisplayName("결과 조회 - CLOSED 배틀, 72시간 경과 → 공개")
    void getResult_closed_after72h_visible() {
        Battle battle = closedBattleWithEndAt(LocalDateTime.now().minusHours(73));
        given(battleRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(battle));
        given(battleVoteRepository.existsByBattleIdAndMemberId(1L, 1L)).willReturn(false);

        VoteResultResponse response = voteService.getResult(1L, 1L);

        assertThat(response.isResultVisible()).isTrue();
    }

    @Test
    @DisplayName("결과 조회 실패 - Battle 없음")
    void getResult_fail_notFound() {
        given(battleRepository.findByIdAndDeletedAtIsNull(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> voteService.getResult(999L, null))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ErrorCode.BATTLE_NOT_FOUND));
    }

    // ===================== getRawVotes =====================

    @Test
    @DisplayName("투표 원본 데이터 조회 성공")
    void getRawVotes_success() {
        Battle battle = closedBattle();
        BattleVote vote = BattleVote.builder().battleId(1L).memberId(1L).selectedOption("A").build();
        given(battleRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(battle));
        given(battleVoteRepository.findByBattleId(1L)).willReturn(List.of(vote));

        VoteRawResponse response = voteService.getRawVotes(1L);

        assertThat(response.getBattleId()).isEqualTo(1L);
        assertThat(response.getVotes()).hasSize(1);
    }

    // ===================== helpers =====================

    private VoteRequest voteRequest(String option) {
        return VoteRequest.builder().option(option).build();
    }

    private Battle pendingBattle() {
        Battle battle = Battle.builder()
                .title("테스트")
                .optionA("A")
                .optionB("B")
                .createdBy(1L)
                .startAt(LocalDateTime.now().minusDays(1))
                .endAt(LocalDateTime.now().plusDays(7))
                .build();
        ReflectionTestUtils.setField(battle, "id", 1L);
        return battle;
    }

    private Battle activeBattle(LocalDateTime startAt, LocalDateTime endAt) {
        Battle battle = Battle.builder()
                .title("테스트")
                .optionA("A")
                .optionB("B")
                .createdBy(1L)
                .startAt(startAt)
                .endAt(endAt)
                .build();
        ReflectionTestUtils.setField(battle, "id", 1L);
        battle.approve();
        return battle;
    }

    private Battle closedBattle() {
        return closedBattleWithEndAt(LocalDateTime.now().minusDays(1));
    }

    private Battle closedBattleWithEndAt(LocalDateTime endAt) {
        Battle battle = Battle.builder()
                .title("테스트")
                .optionA("A")
                .optionB("B")
                .createdBy(1L)
                .startAt(endAt.minusDays(7))
                .endAt(endAt)
                .build();
        ReflectionTestUtils.setField(battle, "id", 1L);
        battle.approve();
        battle.close("A");
        return battle;
    }

    private Battle cancelledBattle() {
        Battle battle = pendingBattle();
        battle.reject();
        return battle;
    }
}
