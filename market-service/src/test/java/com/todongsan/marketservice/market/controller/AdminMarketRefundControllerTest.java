package com.todongsan.marketservice.market.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.todongsan.marketservice.market.client.MemberPointClient;
import com.todongsan.marketservice.market.client.MemberPointRefundBatchRequest;
import com.todongsan.marketservice.market.client.MemberPointRefundBatchResponse;
import com.todongsan.marketservice.market.client.MemberPointRefundItemResult;
import com.todongsan.marketservice.market.client.MemberPointRefundItemStatus;
import com.todongsan.marketservice.market.client.exception.MemberPointTimeoutException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AdminMarketRefundControllerTest {

    private static final long MARKET_ID = 100L;
    private static final long OPTION_ID = 101L;
    private static final long VOID_ID = 500L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private MemberPointClient memberPointClient;

    @BeforeEach
    void setUp() {
        reset(memberPointClient);
        jdbcTemplate.update("DELETE FROM market_price_history");
        jdbcTemplate.update("DELETE FROM market_refund_detail");
        jdbcTemplate.update("DELETE FROM market_settlement_detail");
        jdbcTemplate.update("DELETE FROM market_settlement");
        jdbcTemplate.update("DELETE FROM market_void");
        jdbcTemplate.update("DELETE FROM market_prediction");
        jdbcTemplate.update("DELETE FROM market_option");
        jdbcTemplate.update("DELETE FROM market");
    }

    @Test
    void refundMarketCompletesConfirmedPredictionsAndSendsExpectedPayload() throws Exception {
        insertVoidedMarketWithVoid("PENDING");
        insertOption();
        insertPrediction(1001L, 1L, "100.00", "CONFIRMED");
        insertPrediction(1002L, 2L, "50.00", "CONFIRMED");
        stubRefundResults(Map.of());

        mockMvc.perform(post("/api/v1/admin/markets/{marketId}/refunds", MARKET_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.marketId").value(MARKET_ID))
                .andExpect(jsonPath("$.data.voidId").value(VOID_ID))
                .andExpect(jsonPath("$.data.refundTargetCount").value(2))
                .andExpect(jsonPath("$.data.successCount").value(2))
                .andExpect(jsonPath("$.data.failedCount").value(0))
                .andExpect(jsonPath("$.data.unknownCount").value(0))
                .andExpect(jsonPath("$.data.marketStatus").value("VOIDED"))
                .andExpect(jsonPath("$.data.refundStatus").value("COMPLETED"));

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM market WHERE id = ?", String.class, MARKET_ID))
                .isEqualTo("VOIDED");
        assertThat(jdbcTemplate.queryForObject("SELECT refund_status FROM market_void WHERE id = ?", String.class, VOID_ID))
                .isEqualTo("COMPLETED");
        assertThat(jdbcTemplate.queryForList(
                "SELECT status FROM market_refund_detail ORDER BY prediction_id",
                String.class
        )).containsExactly("SUCCESS", "SUCCESS");
        assertPrediction(1001L, "REFUNDED", "100.00");
        assertPrediction(1002L, "REFUNDED", "50.00");

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MemberPointRefundBatchRequest> requestCaptor =
                ArgumentCaptor.forClass(MemberPointRefundBatchRequest.class);
        verify(memberPointClient).refundMarketPredictions(keyCaptor.capture(), requestCaptor.capture());
        assertThat(keyCaptor.getValue()).isEqualTo("MARKET_REFUND_BATCH:market:100:void:500:attempt:1");
        assertThat(requestCaptor.getValue().refundId()).isEqualTo(keyCaptor.getValue());
        assertThat(requestCaptor.getValue().marketId()).isEqualTo(MARKET_ID);
        assertThat(requestCaptor.getValue().items()).hasSize(2);
        assertThat(requestCaptor.getValue().items().get(0).idempotencyKey())
                .isEqualTo("MARKET_REFUND:market:100:prediction:1001:member:1");
        assertThat(requestCaptor.getValue().items().get(0).referenceType()).isEqualTo("MARKET_PREDICTION");
        assertThat(requestCaptor.getValue().items().get(0).referenceId()).isEqualTo(1001L);
        assertThat(requestCaptor.getValue().items().get(0).reason()).isEqualTo("Market 무효 처리 환불");
        assertThat(requestCaptor.getValue().items().get(0).amount()).isEqualByComparingTo("100.00");
    }

    @Test
    void refundMarketTreatsAlreadyProcessedAsSuccess() throws Exception {
        insertVoidedMarketWithVoid("PENDING");
        insertOption();
        insertPrediction(1001L, 1L, "100.00", "CONFIRMED");
        stubRefundResults(Map.of(1001L, MemberPointRefundItemStatus.ALREADY_PROCESSED));

        mockMvc.perform(post("/api/v1/admin/markets/{marketId}/refunds", MARKET_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.successCount").value(1))
                .andExpect(jsonPath("$.data.failedCount").value(0))
                .andExpect(jsonPath("$.data.unknownCount").value(0))
                .andExpect(jsonPath("$.data.refundStatus").value("COMPLETED"));

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM market_refund_detail", String.class))
                .isEqualTo("SUCCESS");
        assertPrediction(1001L, "REFUNDED", "100.00");
    }

    @Test
    void refundMarketKeepsFailedPredictionsPending() throws Exception {
        insertVoidedMarketWithVoid("PENDING");
        insertOption();
        insertPrediction(1001L, 1L, "100.00", "CONFIRMED");
        insertPrediction(1002L, 2L, "50.00", "CONFIRMED");
        stubRefundResults(Map.of(1002L, MemberPointRefundItemStatus.FAILED));

        mockMvc.perform(post("/api/v1/admin/markets/{marketId}/refunds", MARKET_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.successCount").value(1))
                .andExpect(jsonPath("$.data.failedCount").value(1))
                .andExpect(jsonPath("$.data.unknownCount").value(0))
                .andExpect(jsonPath("$.data.refundStatus").value("IN_PROGRESS"));

        assertThat(jdbcTemplate.queryForList(
                "SELECT status FROM market_refund_detail ORDER BY prediction_id",
                String.class
        )).containsExactly("SUCCESS", "FAILED");
        assertPrediction(1001L, "REFUNDED", "100.00");
        assertPrediction(1002L, "REFUND_PENDING", null);
    }

    @Test
    void refundMarketMarksUnknownOnBatchTimeout() throws Exception {
        insertVoidedMarketWithVoid("PENDING");
        insertOption();
        insertPrediction(1001L, 1L, "100.00", "CONFIRMED");
        when(memberPointClient.refundMarketPredictions(
                anyString(),
                any(MemberPointRefundBatchRequest.class)
        )).thenThrow(new MemberPointTimeoutException("timeout"));

        mockMvc.perform(post("/api/v1/admin/markets/{marketId}/refunds", MARKET_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.successCount").value(0))
                .andExpect(jsonPath("$.data.failedCount").value(0))
                .andExpect(jsonPath("$.data.unknownCount").value(1))
                .andExpect(jsonPath("$.data.refundStatus").value("IN_PROGRESS"));

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM market_refund_detail", String.class))
                .isEqualTo("UNKNOWN");
        assertPrediction(1001L, "REFUND_UNKNOWN", null);
    }

    @Test
    void refundMarketCompletesWithoutTargetsAndSkipsMemberPoint() throws Exception {
        insertVoidedMarketWithVoid("PENDING");
        insertOption();
        insertPrediction(1001L, 1L, "100.00", "FAILED");

        mockMvc.perform(post("/api/v1/admin/markets/{marketId}/refunds", MARKET_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refundTargetCount").value(0))
                .andExpect(jsonPath("$.data.successCount").value(0))
                .andExpect(jsonPath("$.data.failedCount").value(0))
                .andExpect(jsonPath("$.data.unknownCount").value(0))
                .andExpect(jsonPath("$.data.refundStatus").value("COMPLETED"));

        verifyNoInteractions(memberPointClient);
        assertThat(jdbcTemplate.queryForObject("SELECT refund_status FROM market_void WHERE id = ?", String.class, VOID_ID))
                .isEqualTo("COMPLETED");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM market_refund_detail", Integer.class))
                .isZero();
        assertPrediction(1001L, "FAILED", null);
    }

    @Test
    void refundMarketRejectsNonVoidedMarket() throws Exception {
        insertMarket("ACTIVE");

        expectRefundError(MARKET_ID, 409, "MARKET_INVALID_STATUS");

        verifyNoInteractions(memberPointClient);
    }

    @Test
    void refundMarketRejectsMissingMarketVoid() throws Exception {
        insertMarket("VOIDED");

        expectRefundError(MARKET_ID, 409, "MARKET_INVALID_SETTLEMENT_DATA");

        verifyNoInteractions(memberPointClient);
    }

    @Test
    void refundMarketRejectsExistingRefundDetail() throws Exception {
        insertVoidedMarketWithVoid("IN_PROGRESS");
        insertOption();
        insertPrediction(1001L, 1L, "100.00", "REFUND_PENDING");
        insertRefundDetail(9001L, 1001L, 1L, "PENDING");

        expectRefundError(MARKET_ID, 409, "MARKET_ALREADY_REFUNDED");

        verifyNoInteractions(memberPointClient);
    }

    @Test
    void refundMarketExcludesNonConfirmedPredictions() throws Exception {
        insertVoidedMarketWithVoid("PENDING");
        insertOption();
        insertPrediction(1001L, 1L, "100.00", "CONFIRMED");
        insertPrediction(1002L, 2L, "50.00", "FAILED");
        insertPrediction(1003L, 3L, "30.00", "REFUNDED");
        stubRefundResults(Map.of());

        mockMvc.perform(post("/api/v1/admin/markets/{marketId}/refunds", MARKET_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refundTargetCount").value(1))
                .andExpect(jsonPath("$.data.successCount").value(1));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM market_refund_detail", Integer.class))
                .isOne();
        assertPrediction(1001L, "REFUNDED", "100.00");
        assertPrediction(1002L, "FAILED", null);
        assertPrediction(1003L, "REFUNDED", null);
    }

    @Test
    void refundMarketMarksMissingItemResultAsUnknown() throws Exception {
        insertVoidedMarketWithVoid("PENDING");
        insertOption();
        insertPrediction(1001L, 1L, "100.00", "CONFIRMED");
        when(memberPointClient.refundMarketPredictions(
                anyString(),
                any(MemberPointRefundBatchRequest.class)
        )).thenReturn(new MemberPointRefundBatchResponse(MARKET_ID, List.of()));

        mockMvc.perform(post("/api/v1/admin/markets/{marketId}/refunds", MARKET_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unknownCount").value(1))
                .andExpect(jsonPath("$.data.refundStatus").value("IN_PROGRESS"));

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM market_refund_detail", String.class))
                .isEqualTo("UNKNOWN");
        assertPrediction(1001L, "REFUND_UNKNOWN", null);
    }

    private void stubRefundResults(Map<Long, MemberPointRefundItemStatus> statuses) {
        when(memberPointClient.refundMarketPredictions(
                anyString(),
                any(MemberPointRefundBatchRequest.class)
        )).thenAnswer(invocation -> {
            MemberPointRefundBatchRequest request = invocation.getArgument(1);
            return new MemberPointRefundBatchResponse(
                    request.marketId(),
                    request.items().stream()
                            .map(item -> {
                                MemberPointRefundItemStatus status = statuses.getOrDefault(
                                        item.predictionId(),
                                        MemberPointRefundItemStatus.PROCESSED
                                );
                                return new MemberPointRefundItemResult(
                                        item.predictionId(),
                                        item.memberId(),
                                        status,
                                        status == MemberPointRefundItemStatus.FAILED ? "MEMBER_POINT_FAILED" : null,
                                        item.amount(),
                                        null
                                );
                            })
                            .toList()
            );
        });
    }

    private void expectRefundError(long marketId, int statusCode, String errorCode) throws Exception {
        mockMvc.perform(post("/api/v1/admin/markets/{marketId}/refunds", marketId))
                .andExpect(status().is(statusCode))
                .andExpect(jsonPath("$.errorCode").value(errorCode));
    }

    private void insertVoidedMarketWithVoid(String refundStatus) {
        insertMarket("VOIDED");
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("""
                INSERT INTO market_void (
                    id, market_id, reason_type, reason_detail, refund_status,
                    voided_at, created_at, updated_at
                ) VALUES (?, ?, 'DATA_UNAVAILABLE', '공공 데이터 제공 중단', ?, ?, ?, ?)
                """,
                VOID_ID,
                MARKET_ID,
                refundStatus,
                now,
                now,
                now
        );
    }

    private void insertMarket(String status) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("""
                INSERT INTO market (
                    id, title, category, answer_type, judge_data_source, judge_criteria, judge_date,
                    status, close_at, total_pool, initial_virtual_liquidity, created_by, created_at, updated_at
                ) VALUES (?, 'Refund Test Market', 'PRICE_INDEX', 'YES_NO', 'TEST', 'TEST',
                          ?, ?, ?, 0.00, 200.00, 1, ?, ?)
                """,
                MARKET_ID,
                LocalDate.now(),
                status,
                LocalDateTime.now().minusDays(1),
                now,
                now
        );
    }

    private void insertOption() {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("""
                INSERT INTO market_option (
                    id, market_id, option_code, option_text, display_order,
                    virtual_pool_amount, real_pool_amount, total_contract_quantity,
                    current_price, prediction_count, is_result, created_at, updated_at
                ) VALUES (?, ?, 'YES', '예', 1, 100.00, 0.00, 0.00000000, 0.50000000, 0, FALSE, ?, ?)
                """,
                OPTION_ID,
                MARKET_ID,
                now,
                now
        );
    }

    private void insertPrediction(long predictionId, long memberId, String pointAmount, String status) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("""
                INSERT INTO market_prediction (
                    id, market_id, option_id, member_id, point_amount, price_snapshot, contract_quantity,
                    status, point_spend_idempotency_key, attempt_no, refund_amount, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, 0.50000000, 200.00000000, ?, ?, 1, NULL, ?, ?)
                """,
                predictionId,
                MARKET_ID,
                OPTION_ID,
                memberId,
                new BigDecimal(pointAmount),
                status,
                "REFUND_TEST_KEY_" + predictionId,
                now,
                now
        );
    }

    private void insertRefundDetail(long detailId, long predictionId, long memberId, String status) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("""
                INSERT INTO market_refund_detail (
                    id, market_void_id, prediction_id, member_id, refund_amount,
                    status, idempotency_key, fail_reason, created_at, updated_at
                ) VALUES (?, ?, ?, ?, 100.00, ?, ?, NULL, ?, ?)
                """,
                detailId,
                VOID_ID,
                predictionId,
                memberId,
                status,
                "EXISTING_REFUND_KEY_" + predictionId,
                now,
                now
        );
    }

    private void assertPrediction(long predictionId, String status, String refundAmount) {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM market_prediction WHERE id = ?",
                String.class,
                predictionId
        )).isEqualTo(status);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT refund_amount FROM market_prediction WHERE id = ?",
                String.class,
                predictionId
        )).isEqualTo(refundAmount);
    }
}
