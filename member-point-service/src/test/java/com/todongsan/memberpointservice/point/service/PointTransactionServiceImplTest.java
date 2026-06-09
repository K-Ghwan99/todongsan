package com.todongsan.memberpointservice.point.service;

import com.todongsan.memberpointservice.global.exception.CustomException;
import com.todongsan.memberpointservice.global.exception.ErrorCode;
import com.todongsan.memberpointservice.member.repository.MemberRepository;
import com.todongsan.memberpointservice.point.dto.response.TransactionResponse;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PointTransactionServiceImplTest {

    @Mock MemberRepository memberRepository;
    @Mock PointHistoryRepository pointHistoryRepository;
    @InjectMocks PointInternalServiceImpl service;

    private static final String KEY = "MARKET_PREDICTION_SPEND:market:7:member:1:attempt:1";
    private static final Long MEMBER_ID = 1L;
    private static final BigDecimal AMOUNT = new BigDecimal("100.00");
    private static final BigDecimal BALANCE = new BigDecimal("50.00");

    private PointHistory createHistory(PointTransactionStatus status) {
        return PointHistory.builder()
                .memberId(MEMBER_ID)
                .type(PointHistoryType.SPEND_MARKET)
                .amount(AMOUNT)
                .balanceSnapshot(BALANCE)
                .referenceType(PointReferenceType.MARKET_PREDICTION)
                .referenceId(1001L)
                .idempotencyKey(KEY)
                .requestHash("some-hash")
                .status(status)
                .failReason(status == PointTransactionStatus.FAILED ? "POINT_INSUFFICIENT" : null)
                .build();
    }

    @Test
    void getTransaction_PROCESSED() {
        when(pointHistoryRepository.findByIdempotencyKey(KEY))
                .thenReturn(Optional.of(createHistory(PointTransactionStatus.SUCCEEDED)));

        TransactionResponse response = service.getTransaction(KEY);

        assertThat(response.getStatus()).isEqualTo("PROCESSED");
        assertThat(response.getMemberId()).isEqualTo(MEMBER_ID);
        assertThat(response.getType()).isEqualTo("SPEND_MARKET");
        assertThat(response.getAmount()).isEqualTo("100.00");
        assertThat(response.getReferenceType()).isEqualTo("MARKET_PREDICTION");
        assertThat(response.getFailReason()).isNull();
    }

    @Test
    void getTransaction_FAILED() {
        when(pointHistoryRepository.findByIdempotencyKey(KEY))
                .thenReturn(Optional.of(createHistory(PointTransactionStatus.FAILED)));

        TransactionResponse response = service.getTransaction(KEY);

        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getFailReason()).isEqualTo("POINT_INSUFFICIENT");
        assertThat(response.getMemberId()).isEqualTo(MEMBER_ID);
    }

    @Test
    void getTransaction_NOT_FOUND() {
        when(pointHistoryRepository.findByIdempotencyKey(KEY)).thenReturn(Optional.empty());

        TransactionResponse response = service.getTransaction(KEY);

        assertThat(response.getStatus()).isEqualTo("NOT_FOUND");
        assertThat(response.getIdempotencyKey()).isEqualTo(KEY);
        assertThat(response.getMemberId()).isNull();
        assertThat(response.getAmount()).isNull();
    }

    @Test
    void getTransaction_키_없으면_예외() {
        assertThatThrownBy(() -> service.getTransaction(null))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.IDEMPOTENCY_KEY_REQUIRED);
    }

    @Test
    void getTransaction_빈값이면_예외() {
        assertThatThrownBy(() -> service.getTransaction("  "))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.IDEMPOTENCY_KEY_REQUIRED);
    }
}
