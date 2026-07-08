package com.todongsan.battle_service.retry.scheduler;

import com.todongsan.battle_service.client.MemberPointClient;
import com.todongsan.battle_service.global.exception.CustomException;
import com.todongsan.battle_service.global.exception.ErrorCode;
import com.todongsan.battle_service.retry.entity.PointRewardRetryQueue;
import com.todongsan.battle_service.retry.entity.RetryStatus;
import com.todongsan.battle_service.retry.repository.PointRewardRetryQueueRepository;
import com.todongsan.battle_service.vote.entity.BattleVote;
import com.todongsan.battle_service.vote.repository.BattleVoteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RetrySchedulerTest {

    @Mock private PointRewardRetryQueueRepository retryQueueRepository;
    @Mock private MemberPointClient memberPointClient;
    @Mock private BattleVoteRepository battleVoteRepository;

    @InjectMocks
    private RetryScheduler retryScheduler;

    private PointRewardRetryQueue queue(String type, Long battleId, Long memberId) {
        return PointRewardRetryQueue.builder()
                .memberId(memberId)
                .referenceType("BATTLE")
                .referenceId(battleId)
                .type(type)
                .amount(BigDecimal.TEN)
                .idempotencyKey("key:" + type + ":" + battleId + ":" + memberId)
                .build();
    }

    @Test
    @DisplayName("EARN_VOTE_WIN 재시도 성공 → battle_vote.is_rewarded = true 동기화")
    void retry_voteWinSuccess_marksVoteRewarded() {
        PointRewardRetryQueue q = queue("EARN_VOTE_WIN", 1L, 678L);
        given(retryQueueRepository.findByStatusAndRetryCountLessThan(RetryStatus.PENDING, 3))
                .willReturn(List.of(q));

        BattleVote vote = BattleVote.builder().battleId(1L).memberId(678L).selectedOption("A").build();
        given(battleVoteRepository.findByBattleIdAndMemberId(1L, 678L)).willReturn(Optional.of(vote));

        retryScheduler.retryPendingRewards();

        assertThat(q.getStatus()).isEqualTo(RetryStatus.SUCCESS);
        assertThat(vote.isRewarded()).isTrue();
    }

    @Test
    @DisplayName("EARN_VOTE(투표 보상) 재시도 성공 → battle_vote 조회/수정 안 함")
    void retry_voteRewardSuccess_doesNotTouchVote() {
        PointRewardRetryQueue q = queue("EARN_VOTE", 1L, 678L);
        given(retryQueueRepository.findByStatusAndRetryCountLessThan(RetryStatus.PENDING, 3))
                .willReturn(List.of(q));

        retryScheduler.retryPendingRewards();

        assertThat(q.getStatus()).isEqualTo(RetryStatus.SUCCESS);
        verify(battleVoteRepository, never()).findByBattleIdAndMemberId(anyLong(), anyLong());
    }

    @Test
    @DisplayName("재시도 실패 → retry_count 증가, is_rewarded 동기화 안 함")
    void retry_failure_incrementsRetryCount() {
        PointRewardRetryQueue q = queue("EARN_VOTE_WIN", 1L, 678L);
        given(retryQueueRepository.findByStatusAndRetryCountLessThan(RetryStatus.PENDING, 3))
                .willReturn(List.of(q));
        willThrow(new CustomException(ErrorCode.EXTERNAL_SERVICE_TIMEOUT))
                .given(memberPointClient).earnPoint(any());

        retryScheduler.retryPendingRewards();

        assertThat(q.getRetryCount()).isEqualTo(1);
        verify(battleVoteRepository, never()).findByBattleIdAndMemberId(anyLong(), anyLong());
    }
}
