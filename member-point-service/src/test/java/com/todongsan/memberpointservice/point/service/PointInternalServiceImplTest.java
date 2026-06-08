package com.todongsan.memberpointservice.point.service;

import com.todongsan.memberpointservice.global.exception.CustomException;
import com.todongsan.memberpointservice.global.exception.ErrorCode;
import com.todongsan.memberpointservice.global.util.RequestHashUtil;
import com.todongsan.memberpointservice.member.entity.Member;
import com.todongsan.memberpointservice.member.repository.MemberRepository;
import com.todongsan.memberpointservice.point.dto.request.EarnRequest;
import com.todongsan.memberpointservice.point.dto.response.EarnResponse;
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

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PointInternalServiceImplTest {

    @Mock MemberRepository memberRepository;
    @Mock PointHistoryRepository pointHistoryRepository;

    @InjectMocks PointInternalServiceImpl pointInternalServiceImpl;

    private static final String KEY = "test-idempotency-key";
    private static final Long MEMBER_ID = 1L;
    private static final String TYPE = "EARN_VOTE";
    private static final BigDecimal AMOUNT = new BigDecimal("10.00");
    private static final String REF_TYPE = "BATTLE";
    private static final Long REF_ID = 42L;

    private Member createMember() {
        return Member.builder()
                .nickname("테스트유저")
                .email("test@kakao.com")
                .oauthProvider("KAKAO")
                .oauthId("kakao-test")
                .build();
    }

    private EarnRequest createEarnRequest() {
        EarnRequest request = mock(EarnRequest.class);
        lenient().when(request.getMemberId()).thenReturn(MEMBER_ID);
        lenient().when(request.getType()).thenReturn(TYPE);
        lenient().when(request.getAmount()).thenReturn(AMOUNT);
        lenient().when(request.getReferenceType()).thenReturn(REF_TYPE);
        lenient().when(request.getReferenceId()).thenReturn(REF_ID);
        lenient().when(request.getReason()).thenReturn("테스트 이유");
        return request;
    }

    private PointHistory createHistory(String requestHash) {
        return PointHistory.builder()
                .memberId(MEMBER_ID)
                .type(PointHistoryType.EARN_VOTE)
                .amount(AMOUNT)
                .balanceSnapshot(BigDecimal.ZERO)
                .reason("테스트 이유")
                .referenceType(PointReferenceType.BATTLE)
                .referenceId(REF_ID)
                .idempotencyKey(KEY)
                .requestHash(requestHash)
                .status(PointTransactionStatus.SUCCEEDED)
                .build();
    }

    // ─── earn ─────────────────────────────────────────────────

    @Test
    void earn_정상_적립() {
        Member member = createMember();
        EarnRequest request = createEarnRequest();

        when(pointHistoryRepository.findByIdempotencyKey(KEY)).thenReturn(Optional.empty());
        when(memberRepository.findByIdAndDeletedAtIsNull(MEMBER_ID)).thenReturn(Optional.of(member));
        when(memberRepository.earnPoint(eq(MEMBER_ID), any())).thenReturn(1);

        PointResult<EarnResponse> result = pointInternalServiceImpl.earn(KEY, request);

        assertThat(result.alreadyProcessed()).isFalse();
        assertThat(result.data().getMemberId()).isEqualTo(MEMBER_ID);
        assertThat(result.data().getType()).isEqualTo(TYPE);
        assertThat(result.data().getAmount()).isEqualTo("10.00");
    }

    @Test
    void earn_멱등성_동일요청_재시도() {
        EarnRequest request = createEarnRequest();
        String hash = RequestHashUtil.compute(MEMBER_ID, TYPE, AMOUNT, REF_TYPE, REF_ID);

        when(pointHistoryRepository.findByIdempotencyKey(KEY)).thenReturn(Optional.of(createHistory(hash)));

        PointResult<EarnResponse> result = pointInternalServiceImpl.earn(KEY, request);

        assertThat(result.alreadyProcessed()).isTrue();
        assertThat(result.data().getMemberId()).isEqualTo(MEMBER_ID);
    }

    @Test
    void earn_멱등성_키충돌() {
        EarnRequest request = createEarnRequest();

        when(pointHistoryRepository.findByIdempotencyKey(KEY)).thenReturn(Optional.of(createHistory("different-hash")));

        assertThatThrownBy(() -> pointInternalServiceImpl.earn(KEY, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.IDEMPOTENCY_KEY_CONFLICT);
    }

    @Test
    void earn_키_없으면_예외() {
        EarnRequest request = createEarnRequest();

        assertThatThrownBy(() -> pointInternalServiceImpl.earn(null, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.IDEMPOTENCY_KEY_REQUIRED);
    }

    @Test
    void earn_amount_0이하_예외() {
        EarnRequest request = mock(EarnRequest.class);
        when(request.getAmount()).thenReturn(BigDecimal.ZERO);
        when(pointHistoryRepository.findByIdempotencyKey(KEY)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pointInternalServiceImpl.earn(KEY, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POINT_INVALID_AMOUNT);
    }

    @Test
    void earn_잘못된_referenceType_예외() {
        EarnRequest request = mock(EarnRequest.class);
        when(request.getAmount()).thenReturn(AMOUNT);
        when(request.getReferenceType()).thenReturn("INVALID");
        when(pointHistoryRepository.findByIdempotencyKey(KEY)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pointInternalServiceImpl.earn(KEY, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POINT_INVALID_REFERENCE_TYPE);
    }

    @Test
    void earn_회원_없으면_예외() {
        EarnRequest request = mock(EarnRequest.class);
        when(request.getAmount()).thenReturn(AMOUNT);
        when(request.getReferenceType()).thenReturn(REF_TYPE);
        when(request.getType()).thenReturn(TYPE);
        when(request.getMemberId()).thenReturn(999L);
        when(pointHistoryRepository.findByIdempotencyKey(KEY)).thenReturn(Optional.empty());
        when(memberRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pointInternalServiceImpl.earn(KEY, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
    }
}
