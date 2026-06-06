package com.todongsan.memberpointservice.point.service;

import com.todongsan.memberpointservice.global.exception.CustomException;
import com.todongsan.memberpointservice.global.exception.ErrorCode;
import com.todongsan.memberpointservice.member.entity.Member;
import com.todongsan.memberpointservice.member.repository.MemberRepository;
import com.todongsan.memberpointservice.point.dto.response.PointBalanceResponse;
import com.todongsan.memberpointservice.point.dto.response.PointHistoryPageResponse;
import com.todongsan.memberpointservice.point.entity.PointHistory;
import com.todongsan.memberpointservice.point.entity.PointHistoryType;
import com.todongsan.memberpointservice.point.entity.PointReferenceType;
import com.todongsan.memberpointservice.point.entity.PointTransactionStatus;
import com.todongsan.memberpointservice.point.repository.PointHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PointServiceImplTest {

    @Mock MemberRepository memberRepository;
    @Mock PointHistoryRepository pointHistoryRepository;

    @InjectMocks PointServiceImpl pointServiceImpl;

    private Member createMember() {
        return Member.builder()
                .nickname("테스트유저")
                .email("test@kakao.com")
                .oauthProvider("KAKAO")
                .oauthId("kakao-test")
                .build();
    }

    private PointHistory createHistory(PointHistoryType type, String amount) {
        return PointHistory.builder()
                .memberId(1L)
                .type(type)
                .amount(new BigDecimal(amount))
                .balanceSnapshot(new BigDecimal("100.00"))
                .reason("테스트 이유")
                .referenceType(PointReferenceType.BATTLE)
                .referenceId(1L)
                .idempotencyKey("key-" + type.name())
                .status(PointTransactionStatus.SUCCEEDED)
                .build();
    }

    // ─── getBalance ───────────────────────────────────────────

    @Test
    void getBalance_정상_조회() {
        when(memberRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(createMember()));

        PointBalanceResponse result = pointServiceImpl.getBalance(1L);

        assertThat(result).isNotNull();
        assertThat(result.getPointBalance()).isNotNull();
    }

    @Test
    void getBalance_회원_없으면_예외() {
        when(memberRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pointServiceImpl.getBalance(999L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
    }

    // ─── getHistory ───────────────────────────────────────────

    @Test
    void getHistory_타입_없이_전체_조회() {
        PointHistory h1 = createHistory(PointHistoryType.EARN_VOTE, "10.00");
        PointHistory h2 = createHistory(PointHistoryType.SPEND_MARKET, "100.00");

        when(memberRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(createMember()));
        when(pointHistoryRepository.findByMemberIdAndStatusOrderByCreatedAtDesc(eq(1L), eq(PointTransactionStatus.SUCCEEDED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(h1, h2)));

        PointHistoryPageResponse result = pointServiceImpl.getHistory(1L, null, 0, 20);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getTotalPages()).isEqualTo(1);
        assertThat(result.getCurrentPage()).isZero();
    }

    @Test
    void getHistory_EARN_타입_필터() {
        PointHistory h = createHistory(PointHistoryType.EARN_VOTE, "10.00");

        when(memberRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(createMember()));
        when(pointHistoryRepository.findByMemberIdAndTypeStartingWithAndStatusSucceeded(eq(1L), eq("EARN_"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(h)));

        PointHistoryPageResponse result = pointServiceImpl.getHistory(1L, "EARN", 0, 20);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getType()).isEqualTo("EARN_VOTE");
        assertThat(result.getContent().get(0).getAmount()).isEqualTo("10.00");
    }

    @Test
    void getHistory_SPEND_타입_필터() {
        PointHistory h = createHistory(PointHistoryType.SPEND_MARKET, "100.00");

        when(memberRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(createMember()));
        when(pointHistoryRepository.findByMemberIdAndTypeStartingWithAndStatusSucceeded(eq(1L), eq("SPEND_"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(h)));

        PointHistoryPageResponse result = pointServiceImpl.getHistory(1L, "SPEND", 0, 20);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getType()).isEqualTo("SPEND_MARKET");
    }

    @Test
    void getHistory_소문자_타입도_동작() {
        when(memberRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(createMember()));
        when(pointHistoryRepository.findByMemberIdAndTypeStartingWithAndStatusSucceeded(eq(1L), eq("EARN_"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        PointHistoryPageResponse result = pointServiceImpl.getHistory(1L, "earn", 0, 20);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void getHistory_잘못된_타입이면_예외() {
        when(memberRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(createMember()));

        assertThatThrownBy(() -> pointServiceImpl.getHistory(1L, "INVALID", 0, 20))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_FAILED);
    }

    @Test
    void getHistory_회원_없으면_예외() {
        when(memberRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pointServiceImpl.getHistory(999L, null, 0, 20))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    void getHistory_빈_결과() {
        when(memberRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(createMember()));
        when(pointHistoryRepository.findByMemberIdAndStatusOrderByCreatedAtDesc(eq(1L), eq(PointTransactionStatus.SUCCEEDED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        PointHistoryPageResponse result = pointServiceImpl.getHistory(1L, null, 0, 20);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }
}
