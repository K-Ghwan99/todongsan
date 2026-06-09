package com.todongsan.memberpointservice.point.service;

import com.todongsan.memberpointservice.global.exception.CustomException;
import com.todongsan.memberpointservice.global.exception.ErrorCode;
import com.todongsan.memberpointservice.global.util.RequestHashUtil;
import com.todongsan.memberpointservice.member.entity.Member;
import com.todongsan.memberpointservice.member.repository.MemberRepository;
import com.todongsan.memberpointservice.point.dto.request.RefundItem;
import com.todongsan.memberpointservice.point.dto.request.RefundRequest;
import com.todongsan.memberpointservice.point.dto.request.SettlementItem;
import com.todongsan.memberpointservice.point.dto.request.SettlementRequest;
import com.todongsan.memberpointservice.point.dto.response.RefundResponse;
import com.todongsan.memberpointservice.point.dto.response.SettlementResponse;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PointBatchServiceImplTest {

    @Mock MemberRepository memberRepository;
    @Mock PointHistoryRepository pointHistoryRepository;
    @InjectMocks PointInternalServiceImpl service;

    private static final String SETTLEMENT_ID = "settle-market-7-20260528";
    private static final String REFUND_ID = "refund-market-7-20260528";
    private static final Long MEMBER_ID = 1L;
    private static final Long PREDICTION_ID = 1001L;
    private static final BigDecimal AMOUNT = new BigDecimal("190.00");
    private static final String ITEM_KEY = "MARKET_SETTLEMENT_REWARD:market:7:prediction:1001:member:1";

    private Member createMember() {
        return Member.builder()
                .nickname("테스트유저")
                .email("test@kakao.com")
                .oauthProvider("KAKAO")
                .oauthId("kakao-test")
                .build();
    }

    private SettlementItem createSettlementItem() {
        SettlementItem item = mock(SettlementItem.class);
        lenient().when(item.getPredictionId()).thenReturn(PREDICTION_ID);
        lenient().when(item.getMemberId()).thenReturn(MEMBER_ID);
        lenient().when(item.getAmount()).thenReturn(AMOUNT);
        lenient().when(item.getReferenceType()).thenReturn("MARKET_PREDICTION");
        lenient().when(item.getReferenceId()).thenReturn(PREDICTION_ID);
        lenient().when(item.getReason()).thenReturn("Market 정산 보상");
        lenient().when(item.getIdempotencyKey()).thenReturn(ITEM_KEY);
        return item;
    }

    private SettlementRequest createSettlementRequest(List<SettlementItem> items) {
        SettlementRequest req = mock(SettlementRequest.class);
        when(req.getSettlementId()).thenReturn(SETTLEMENT_ID);
        lenient().when(req.getMarketId()).thenReturn(7L);
        when(req.getItems()).thenReturn(items);
        return req;
    }

    private PointHistory createSettlementHistory(String requestHash) {
        return PointHistory.builder()
                .memberId(MEMBER_ID)
                .type(PointHistoryType.SETTLE_MARKET)
                .amount(AMOUNT)
                .balanceSnapshot(new BigDecimal("340.00"))
                .reason("Market 정산 보상")
                .referenceType(PointReferenceType.MARKET_PREDICTION)
                .referenceId(PREDICTION_ID)
                .idempotencyKey(ITEM_KEY)
                .requestHash(requestHash)
                .status(PointTransactionStatus.SUCCEEDED)
                .build();
    }

    // ─── settle ───────────────────────────────────────────────

    @Test
    void settle_정상_처리() {
        Member member = createMember();
        SettlementItem item = createSettlementItem();
        SettlementRequest request = createSettlementRequest(List.of(item));

        when(pointHistoryRepository.findByIdempotencyKey(ITEM_KEY)).thenReturn(Optional.empty());
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
        when(memberRepository.earnPoint(eq(MEMBER_ID), any())).thenReturn(1);

        SettlementResponse response = service.settle(SETTLEMENT_ID, request);

        assertThat(response.getResults()).hasSize(1);
        assertThat(response.getResults().get(0).getStatus()).isEqualTo("PROCESSED");
        assertThat(response.getResults().get(0).getMemberId()).isEqualTo(MEMBER_ID);
    }

    @Test
    void settle_멱등성_ALREADY_PROCESSED() {
        String hash = RequestHashUtil.compute(MEMBER_ID, "SETTLE_MARKET", AMOUNT, "MARKET_PREDICTION", PREDICTION_ID);
        SettlementItem item = createSettlementItem();
        SettlementRequest request = createSettlementRequest(List.of(item));

        when(pointHistoryRepository.findByIdempotencyKey(ITEM_KEY))
                .thenReturn(Optional.of(createSettlementHistory(hash)));

        SettlementResponse response = service.settle(SETTLEMENT_ID, request);

        assertThat(response.getResults().get(0).getStatus()).isEqualTo("ALREADY_PROCESSED");
        assertThat(response.getResults().get(0).getMemberId()).isEqualTo(MEMBER_ID);
    }

    @Test
    void settle_멱등성_KEY_CONFLICT() {
        SettlementItem item = createSettlementItem();
        SettlementRequest request = createSettlementRequest(List.of(item));

        when(pointHistoryRepository.findByIdempotencyKey(ITEM_KEY))
                .thenReturn(Optional.of(createSettlementHistory("different-hash")));

        SettlementResponse response = service.settle(SETTLEMENT_ID, request);

        assertThat(response.getResults().get(0).getStatus()).isEqualTo("FAILED");
        assertThat(response.getResults().get(0).getErrorCode()).isEqualTo("IDEMPOTENCY_KEY_CONFLICT");
    }

    @Test
    void settle_부분_실패_MEMBER_NOT_FOUND() {
        Member member = createMember();
        SettlementItem item1 = createSettlementItem();

        SettlementItem item2 = mock(SettlementItem.class);
        lenient().when(item2.getPredictionId()).thenReturn(1002L);
        lenient().when(item2.getMemberId()).thenReturn(999L);
        lenient().when(item2.getAmount()).thenReturn(AMOUNT);
        lenient().when(item2.getReferenceType()).thenReturn("MARKET_PREDICTION");
        lenient().when(item2.getReferenceId()).thenReturn(1002L);
        lenient().when(item2.getIdempotencyKey()).thenReturn("key-item2");

        SettlementRequest request = createSettlementRequest(List.of(item1, item2));

        when(pointHistoryRepository.findByIdempotencyKey(ITEM_KEY)).thenReturn(Optional.empty());
        when(pointHistoryRepository.findByIdempotencyKey("key-item2")).thenReturn(Optional.empty());
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
        when(memberRepository.findById(999L)).thenReturn(Optional.empty());
        when(memberRepository.earnPoint(eq(MEMBER_ID), any())).thenReturn(1);

        SettlementResponse response = service.settle(SETTLEMENT_ID, request);

        assertThat(response.getResults()).hasSize(2);
        assertThat(response.getResults().get(0).getStatus()).isEqualTo("PROCESSED");
        assertThat(response.getResults().get(1).getStatus()).isEqualTo("FAILED");
        assertThat(response.getResults().get(1).getErrorCode()).isEqualTo("MEMBER_NOT_FOUND");
    }

    @Test
    void settle_Header_body_불일치_예외() {
        SettlementRequest request = mock(SettlementRequest.class);
        when(request.getSettlementId()).thenReturn(SETTLEMENT_ID);

        assertThatThrownBy(() -> service.settle("wrong-key", request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST);
    }

    @Test
    void settle_키_없으면_예외() {
        SettlementRequest request = mock(SettlementRequest.class);

        assertThatThrownBy(() -> service.settle(null, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.IDEMPOTENCY_KEY_REQUIRED);
    }

    @Test
    void settle_item_키_없으면_예외() {
        SettlementItem item = mock(SettlementItem.class);
        when(item.getIdempotencyKey()).thenReturn(null);

        SettlementRequest request = mock(SettlementRequest.class);
        when(request.getSettlementId()).thenReturn(SETTLEMENT_ID);
        when(request.getItems()).thenReturn(List.of(item));

        assertThatThrownBy(() -> service.settle(SETTLEMENT_ID, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.IDEMPOTENCY_KEY_REQUIRED);
    }

    // ─── refund ───────────────────────────────────────────────

    private RefundItem createRefundItem(String refType, String itemKey) {
        RefundItem item = mock(RefundItem.class);
        lenient().when(item.getPredictionId()).thenReturn(PREDICTION_ID);
        lenient().when(item.getMemberId()).thenReturn(MEMBER_ID);
        lenient().when(item.getAmount()).thenReturn(new BigDecimal("100.00"));
        lenient().when(item.getReferenceType()).thenReturn(refType);
        lenient().when(item.getReferenceId()).thenReturn(PREDICTION_ID);
        lenient().when(item.getReason()).thenReturn("환불");
        lenient().when(item.getIdempotencyKey()).thenReturn(itemKey);
        return item;
    }

    private RefundRequest createRefundRequest(List<RefundItem> items) {
        RefundRequest req = mock(RefundRequest.class);
        when(req.getRefundId()).thenReturn(REFUND_ID);
        lenient().when(req.getMarketId()).thenReturn(7L);
        when(req.getItems()).thenReturn(items);
        return req;
    }

    @Test
    void refund_정상_처리_Market() {
        Member member = createMember();
        RefundItem item = createRefundItem("MARKET_PREDICTION", "refund-key-1");
        RefundRequest request = createRefundRequest(List.of(item));

        when(pointHistoryRepository.findByIdempotencyKey("refund-key-1")).thenReturn(Optional.empty());
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
        when(memberRepository.earnPoint(eq(MEMBER_ID), any())).thenReturn(1);

        RefundResponse response = service.refund(REFUND_ID, request);

        assertThat(response.getResults().get(0).getStatus()).isEqualTo("PROCESSED");
        assertThat(response.getResults().get(0).getMemberId()).isEqualTo(MEMBER_ID);
    }

    @Test
    void refund_정상_처리_Insight() {
        Member member = createMember();
        RefundItem item = createRefundItem("INSIGHT_REPORT", "insight-refund-key-1");
        RefundRequest request = createRefundRequest(List.of(item));

        when(pointHistoryRepository.findByIdempotencyKey("insight-refund-key-1")).thenReturn(Optional.empty());
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
        when(memberRepository.earnPoint(eq(MEMBER_ID), any())).thenReturn(1);

        RefundResponse response = service.refund(REFUND_ID, request);

        assertThat(response.getResults().get(0).getStatus()).isEqualTo("PROCESSED");
        assertThat(response.getResults().get(0).getMemberId()).isEqualTo(MEMBER_ID);
    }

    @Test
    void refund_Header_body_불일치_예외() {
        RefundRequest request = mock(RefundRequest.class);
        when(request.getRefundId()).thenReturn(REFUND_ID);

        assertThatThrownBy(() -> service.refund("wrong-key", request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REQUEST);
    }
}
