package com.todongsan.marketservice.market.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.todongsan.marketservice.market.client.MemberPointClient;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AdminMarketVoidControllerTest {

    private static final long MARKET_ID = 100L;
    private static final long OPTION_ID = 101L;

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
    void voidPendingMarketSucceedsWithoutRefundTarget() throws Exception {
        insertMarket("PENDING");

        mockMvc.perform(patch("/api/v1/admin/markets/{marketId}/void", MARKET_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(voidRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.marketId").value(MARKET_ID))
                .andExpect(jsonPath("$.data.voidId").isNumber())
                .andExpect(jsonPath("$.data.status").value("VOIDED"))
                .andExpect(jsonPath("$.data.refundRequired").value(false))
                .andExpect(jsonPath("$.data.refundablePredictionCount").value(0))
                .andExpect(jsonPath("$.data.reasonCode").value("DATA_UNAVAILABLE"))
                .andExpect(jsonPath("$.data.reason").value("공공 데이터 제공 중단"));

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM market WHERE id = ?", String.class, MARKET_ID))
                .isEqualTo("VOIDED");
        assertMarketVoid("DATA_UNAVAILABLE", "공공 데이터 제공 중단", "PENDING");
        verifyNoInteractions(memberPointClient);
    }

    @Test
    void voidActiveMarketSucceedsWithRefundTargetsAndKeepsPredictions() throws Exception {
        insertMarket("ACTIVE");
        insertOption();
        insertPrediction(1001L, 1L, "CONFIRMED");
        insertPrediction(1002L, 2L, "CONFIRMED");
        insertPrediction(1003L, 3L, "FAILED");

        mockMvc.perform(patch("/api/v1/admin/markets/{marketId}/void", MARKET_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(voidRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("VOIDED"))
                .andExpect(jsonPath("$.data.refundRequired").value(true))
                .andExpect(jsonPath("$.data.refundablePredictionCount").value(2));

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM market WHERE id = ?", String.class, MARKET_ID))
                .isEqualTo("VOIDED");
        assertThat(jdbcTemplate.queryForList(
                "SELECT status FROM market_prediction ORDER BY id",
                String.class
        )).containsExactly("CONFIRMED", "CONFIRMED", "FAILED");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM market_void", Integer.class)).isOne();
        verifyNoInteractions(memberPointClient);
    }

    @ParameterizedTest
    @ValueSource(strings = {"CLOSED", "DATA_PENDING"})
    void voidMarketSucceedsForClosedAndDataPending(String marketStatus) throws Exception {
        insertMarket(marketStatus);

        mockMvc.perform(patch("/api/v1/admin/markets/{marketId}/void", MARKET_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(voidRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("VOIDED"));

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM market WHERE id = ?", String.class, MARKET_ID))
                .isEqualTo("VOIDED");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM market_void", Integer.class)).isOne();
        verifyNoInteractions(memberPointClient);
    }

    @ParameterizedTest
    @ValueSource(strings = {"SETTLEMENT_IN_PROGRESS", "SETTLED", "VOIDED"})
    void voidMarketRejectsNonVoidableStatuses(String marketStatus) throws Exception {
        insertMarket(marketStatus);

        expectVoidError(MARKET_ID, voidRequest(), 409, "MARKET_CANNOT_VOID");

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM market WHERE id = ?", String.class, MARKET_ID))
                .isEqualTo(marketStatus);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM market_void", Integer.class)).isZero();
        verifyNoInteractions(memberPointClient);
    }

    @ParameterizedTest
    @ValueSource(strings = {"POINT_PENDING", "POINT_UNKNOWN"})
    void voidMarketRejectsUnresolvedPredictions(String predictionStatus) throws Exception {
        insertMarket("ACTIVE");
        insertOption();
        insertPrediction(1001L, 1L, predictionStatus);

        expectVoidError(MARKET_ID, voidRequest(), 409, "MARKET_INVALID_STATUS");

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM market WHERE id = ?", String.class, MARKET_ID))
                .isEqualTo("ACTIVE");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM market_void", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM market_prediction", String.class))
                .isEqualTo(predictionStatus);
        verifyNoInteractions(memberPointClient);
    }

    @Test
    void voidMarketRejectsMissingMarket() throws Exception {
        expectVoidError(999L, voidRequest(), 404, "MARKET_NOT_FOUND");

        verifyNoInteractions(memberPointClient);
    }

    @Test
    void voidMarketRejectsBlankRequest() throws Exception {
        insertMarket("ACTIVE");

        expectVoidError(MARKET_ID, """
                {
                  "reasonCode": " ",
                  "reason": " "
                }
                """, 400, "VALIDATION_FAILED");

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM market WHERE id = ?", String.class, MARKET_ID))
                .isEqualTo("ACTIVE");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM market_void", Integer.class)).isZero();
        verifyNoInteractions(memberPointClient);
    }

    @Test
    void voidMarketRejectsUnknownReasonCode() throws Exception {
        insertMarket("ACTIVE");

        expectVoidError(MARKET_ID, """
                {
                  "reasonCode": "UNKNOWN_REASON",
                  "reason": "공공 데이터 제공 중단"
                }
                """, 400, "VALIDATION_FAILED");

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM market WHERE id = ?", String.class, MARKET_ID))
                .isEqualTo("ACTIVE");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM market_void", Integer.class)).isZero();
        verifyNoInteractions(memberPointClient);
    }

    @Test
    void voidMarketRejectsExistingMarketVoid() throws Exception {
        insertMarket("ACTIVE");
        insertMarketVoid();

        expectVoidError(MARKET_ID, voidRequest(), 409, "MARKET_CANNOT_VOID");

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM market WHERE id = ?", String.class, MARKET_ID))
                .isEqualTo("ACTIVE");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM market_void", Integer.class)).isOne();
        verifyNoInteractions(memberPointClient);
    }

    private void expectVoidError(long marketId, String request, int statusCode, String errorCode) throws Exception {
        mockMvc.perform(patch("/api/v1/admin/markets/{marketId}/void", marketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().is(statusCode))
                .andExpect(jsonPath("$.errorCode").value(errorCode));
    }

    private void insertMarket(String status) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("""
                INSERT INTO market (
                    id, title, category, answer_type, judge_data_source, judge_criteria, judge_date,
                    status, close_at, total_pool, initial_virtual_liquidity, created_by, created_at, updated_at
                ) VALUES (?, 'Void Test Market', 'PRICE_INDEX', 'YES_NO', 'TEST', 'TEST',
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

    private void insertPrediction(long predictionId, long memberId, String status) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("""
                INSERT INTO market_prediction (
                    id, market_id, option_id, member_id, point_amount, price_snapshot, contract_quantity,
                    status, point_spend_idempotency_key, attempt_no, created_at, updated_at
                ) VALUES (?, ?, ?, ?, 100.00, ?, ?, ?, ?, 1, ?, ?)
                """,
                predictionId,
                MARKET_ID,
                OPTION_ID,
                memberId,
                status.equals("CONFIRMED") ? new BigDecimal("0.50000000") : null,
                status.equals("CONFIRMED") ? new BigDecimal("200.00000000") : null,
                status,
                "VOID_TEST_KEY_" + predictionId,
                now,
                now
        );
    }

    private void insertMarketVoid() {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("""
                INSERT INTO market_void (
                    market_id, reason_type, reason_detail, refund_status,
                    voided_at, created_at, updated_at
                ) VALUES (?, 'DATA_UNAVAILABLE', '이미 무효 처리됨', 'PENDING', ?, ?, ?)
                """,
                MARKET_ID,
                now,
                now,
                now
        );
    }

    private void assertMarketVoid(String reasonType, String reasonDetail, String refundStatus) {
        assertThat(jdbcTemplate.queryForObject("SELECT reason_type FROM market_void", String.class))
                .isEqualTo(reasonType);
        assertThat(jdbcTemplate.queryForObject("SELECT reason_detail FROM market_void", String.class))
                .isEqualTo(reasonDetail);
        assertThat(jdbcTemplate.queryForObject("SELECT refund_status FROM market_void", String.class))
                .isEqualTo(refundStatus);
        assertThat(jdbcTemplate.queryForObject("SELECT voided_by FROM market_void", Long.class))
                .isNull();
        assertThat(jdbcTemplate.queryForObject("SELECT voided_at FROM market_void", LocalDateTime.class))
                .isNotNull();
    }

    private String voidRequest() {
        return """
                {
                  "reasonCode": "DATA_UNAVAILABLE",
                  "reason": "공공 데이터 제공 중단"
                }
                """;
    }
}
