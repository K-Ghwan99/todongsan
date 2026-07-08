package com.todongsan.marketservice.market.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.todongsan.marketservice.market.client.MemberPointClient;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@AutoConfigureMockMvc
class AdminMarketQueryControllerTest {

    @Autowired
    private MockMvc rawMockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private MemberPointClient memberPointClient;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .defaultRequest(get("/").header("X-Member-Role", "ADMIN"))
                .build();
        jdbcTemplate.update("DELETE FROM market_price_history");
        jdbcTemplate.update("DELETE FROM market_reputation_update");
        jdbcTemplate.update("DELETE FROM market_refund_detail");
        jdbcTemplate.update("DELETE FROM market_settlement_detail");
        jdbcTemplate.update("DELETE FROM market_settlement");
        jdbcTemplate.update("DELETE FROM market_void");
        jdbcTemplate.update("DELETE FROM market_prediction");
        jdbcTemplate.update("DELETE FROM market_option");
        jdbcTemplate.update("DELETE FROM market");
    }

    @Test
    void getAdminMarketReturnsFullDetailAndEmptySummaries() throws Exception {
        insertMarket(1L, "ACTIVE", LocalDateTime.now().minusMinutes(1), "Admin Detail", "300.00");
        insertOption(11L, 1L, "LOW", "낮음", 1, null, "0.0000", false,
                "100.00", "100.00", "0.40000000", 1);
        insertOption(12L, 1L, "HIGH", "높음", 2, "0.0000", null, false,
                "100.00", "200.00", "0.60000000", 2);

        mockMvc.perform(get("/api/v1/admin/markets/{marketId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.answerType").value("NUMERIC_RANGE"))
                .andExpect(jsonPath("$.data.category").value("PRICE_INDEX"))
                .andExpect(jsonPath("$.data.metricUnit").value("PERCENT"))
                .andExpect(jsonPath("$.data.regionScope").value("NON_REGIONAL"))
                .andExpect(jsonPath("$.data.regionSido").isEmpty())
                .andExpect(jsonPath("$.data.regionSigu").isEmpty())
                .andExpect(jsonPath("$.data.judgeDataSource").value("TEST_SOURCE"))
                .andExpect(jsonPath("$.data.judgeCriteria").value("TEST_CRITERIA"))
                .andExpect(jsonPath("$.data.feeRate").value("5.00"))
                .andExpect(jsonPath("$.data.displayStatus").value("CLOSED_BY_TIME"))
                .andExpect(jsonPath("$.data.canPredict").value(false))
                .andExpect(jsonPath("$.data.totalVirtualPoolAmount").value("200.00"))
                .andExpect(jsonPath("$.data.totalEffectivePoolAmount").value("500.00"))
                .andExpect(jsonPath("$.data.totalPredictionCount").value(3))
                .andExpect(jsonPath("$.data.options[0].rangeMax").value("0.0000"))
                .andExpect(jsonPath("$.data.options[1].rangeMin").value("0.0000"))
                .andExpect(jsonPath("$.data.options[0].initialPrice").value("0.50000000"))
                .andExpect(jsonPath("$.data.options[0].priceChangeRate").value("-20.00000000"))
                .andExpect(jsonPath("$.data.settlementSummary.settlementId").isEmpty())
                .andExpect(jsonPath("$.data.settlementSummary.totalDetailCount").value(0))
                .andExpect(jsonPath("$.data.refundSummary.voidId").isEmpty())
                .andExpect(jsonPath("$.data.refundSummary.refundRequired").value(false));
    }

    @Test
    void getSettlementSummaryAndFilteredDetails() throws Exception {
        insertMarket(1L, "SETTLEMENT_IN_PROGRESS", LocalDateTime.now().minusDays(1), "Settlement", "200.00");
        insertOption(11L, 1L, "YES", "예", 1, null, null, true,
                "100.00", "100.00", "0.50000000", 1);
        insertOption(12L, 1L, "NO", "아니오", 2, null, null, false,
                "100.00", "100.00", "0.50000000", 1);
        insertPrediction(101L, 1L, 11L, 1L, "CONFIRMED", LocalDateTime.now());
        insertPrediction(102L, 1L, 11L, 2L, "CONFIRMED", LocalDateTime.now());
        jdbcTemplate.update("UPDATE market_prediction SET point_amount = 499.00 WHERE id = ?", 102L);
        insertSettlement(501L, 1L, 11L, "IN_PROGRESS");
        insertSettlementDetail(601L, 501L, 101L, 1L, "SUCCESS", null);
        insertSettlementDetail(602L, 501L, 102L, 2L, "FAILED", "MEMBER_POINT_FAILED");

        mockMvc.perform(get("/api/v1/admin/markets/{marketId}/settlements", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.settlementId").value(501))
                .andExpect(jsonPath("$.data.resultOptionText").value("예"))
                .andExpect(jsonPath("$.data.totalDetailCount").value(2))
                .andExpect(jsonPath("$.data.successCount").value(1))
                .andExpect(jsonPath("$.data.failedCount").value(1))
                .andExpect(jsonPath("$.data.totalPool").value("200.00"));

        mockMvc.perform(get("/api/v1/admin/markets/{marketId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.settlementSummary.settlementId").value(501))
                .andExpect(jsonPath("$.data.settlementSummary.successCount").value(1))
                .andExpect(jsonPath("$.data.settlementSummary.failedCount").value(1));

        mockMvc.perform(get("/api/v1/admin/markets/{marketId}/settlements/{settlementId}/details", 1L, 501L)
                        .param("status", "FAILED")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].settlementId").value(501))
                .andExpect(jsonPath("$.data.content[0].selectedOptionId").value(11))
                .andExpect(jsonPath("$.data.content[0].pointAmount").value("100.00"))
                .andExpect(jsonPath("$.data.content[0].contractQuantity").value("200.00000000"))
                .andExpect(jsonPath("$.data.content[0].status").value("FAILED"))
                .andExpect(jsonPath("$.data.content[0].settledAmount").value("195.00"))
                .andExpect(jsonPath("$.data.content[0].profitAmount").value("95.00"))
                .andExpect(jsonPath("$.data.content[0].failureReason").value("MEMBER_POINT_FAILED"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void getSettlementWithoutRowReturnsEmptySummary() throws Exception {
        insertMarket(1L, "CLOSED", LocalDateTime.now().minusDays(1), "No Settlement", "0.00");

        mockMvc.perform(get("/api/v1/admin/markets/{marketId}/settlements", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.settlementId").isEmpty())
                .andExpect(jsonPath("$.data.totalPool").value("0.00"))
                .andExpect(jsonPath("$.data.totalDetailCount").value(0));
    }

    @Test
    void getRefundSummaryAndFilteredDetails() throws Exception {
        insertMarket(1L, "VOIDED", LocalDateTime.now().minusDays(1), "Refund", "200.00");
        insertOption(11L, 1L, "YES", "예", 1, null, null, false,
                "100.00", "200.00", "0.50000000", 2);
        insertPrediction(101L, 1L, 11L, 1L, "REFUND_PENDING", LocalDateTime.now());
        insertPrediction(102L, 1L, 11L, 2L, "REFUND_UNKNOWN", LocalDateTime.now());
        insertVoid(701L, 1L, "IN_PROGRESS");
        insertRefundDetail(801L, 701L, 101L, 1L, "SUCCESS", null, LocalDateTime.now());
        insertRefundDetail(802L, 701L, 102L, 2L, "UNKNOWN", "UNKNOWN_RESULT", LocalDateTime.now());

        mockMvc.perform(get("/api/v1/admin/markets/{marketId}/refunds", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.voidId").value(701))
                .andExpect(jsonPath("$.data.refundRequired").value(true))
                .andExpect(jsonPath("$.data.totalRefundAmount").value("200.00"))
                .andExpect(jsonPath("$.data.successCount").value(1))
                .andExpect(jsonPath("$.data.unknownCount").value(1));

        mockMvc.perform(get("/api/v1/admin/markets/{marketId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refundSummary.voidId").value(701))
                .andExpect(jsonPath("$.data.refundSummary.successCount").value(1))
                .andExpect(jsonPath("$.data.refundSummary.unknownCount").value(1));

        mockMvc.perform(get("/api/v1/admin/markets/{marketId}/refunds/{voidId}/details", 1L, 701L)
                        .param("status", "UNKNOWN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].voidId").value(701))
                .andExpect(jsonPath("$.data.content[0].pointAmount").value("100.00"))
                .andExpect(jsonPath("$.data.content[0].refundAmount").value("100.00"))
                .andExpect(jsonPath("$.data.content[0].status").value("UNKNOWN"))
                .andExpect(jsonPath("$.data.content[0].failureReason").value("UNKNOWN_RESULT"))
                .andExpect(jsonPath("$.data.content[0].settlementId").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].settledAmount").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].profitAmount").doesNotExist());
    }

    @Test
    void getRefundWithoutVoidReturnsEmptySummary() throws Exception {
        insertMarket(1L, "ACTIVE", LocalDateTime.now().plusDays(1), "No Refund", "0.00");

        mockMvc.perform(get("/api/v1/admin/markets/{marketId}/refunds", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.voidId").isEmpty())
                .andExpect(jsonPath("$.data.refundRequired").value(false))
                .andExpect(jsonPath("$.data.totalRefundAmount").value("0.00"));
    }

    @Test
    void getProblemMarketsReturnsAllTypesAndSupportsFilter() throws Exception {
        insertSettlementProblem(1L);
        insertRefundProblem(2L);
        insertPredictionProblem(3L);
        insertReputationProblem(4L);

        mockMvc.perform(get("/api/v1/admin/markets/problem-markets").param("type", "ALL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].problemType")
                        .value(hasItems("SETTLEMENT", "REFUND", "PREDICTION_RECONCILE", "REPUTATION")))
                .andExpect(jsonPath("$.data.totalElements").value(4))
                .andExpect(jsonPath("$.data.content[?(@.problemType == 'REPUTATION')].manualCheckRequired")
                        .value(hasItem(true)))
                .andExpect(jsonPath("$.data.content[?(@.problemType == 'REPUTATION')].autoRecoverable")
                        .value(hasItem(false)));

        mockMvc.perform(get("/api/v1/admin/markets/problem-markets").param("type", "SETTLEMENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].problemType").value("SETTLEMENT"))
                .andExpect(jsonPath("$.data.content[0].problemStatus").value("FAILED"));
    }

    @Test
    void getStatusCountsSeparatesClosedByTimeAndCountsProblems() throws Exception {
        insertMarket(1L, "PENDING", LocalDateTime.now().plusDays(1), "PENDING", "0.00");
        insertMarket(2L, "ACTIVE", LocalDateTime.now().plusDays(1), "ACTIVE", "0.00");
        insertMarket(3L, "ACTIVE", LocalDateTime.now().minusMinutes(1), "CLOSED_BY_TIME", "0.00");
        insertMarket(4L, "CLOSED", LocalDateTime.now().minusDays(1), "CLOSED", "0.00");
        insertMarket(5L, "DATA_PENDING", LocalDateTime.now().minusDays(1), "DATA_PENDING", "0.00");
        insertMarket(6L, "SETTLEMENT_IN_PROGRESS", LocalDateTime.now().minusDays(1), "SIP", "0.00");
        insertMarket(7L, "SETTLED", LocalDateTime.now().minusDays(1), "SETTLED", "0.00");
        insertMarket(8L, "VOIDED", LocalDateTime.now().minusDays(1), "VOIDED", "0.00");
        insertOption(21L, 2L, "YES", "예", 1, null, null, false,
                "100.00", "0.00", "0.50000000", 0);
        insertPrediction(201L, 2L, 21L, 1L, "POINT_UNKNOWN", LocalDateTime.now());

        mockMvc.perform(get("/api/v1/admin/markets/status-counts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(8))
                .andExpect(jsonPath("$.data.pending").value(1))
                .andExpect(jsonPath("$.data.active").value(1))
                .andExpect(jsonPath("$.data.closedByTime").value(1))
                .andExpect(jsonPath("$.data.closed").value(1))
                .andExpect(jsonPath("$.data.dataPending").value(1))
                .andExpect(jsonPath("$.data.settlementInProgress").value(1))
                .andExpect(jsonPath("$.data.settled").value(1))
                .andExpect(jsonPath("$.data.voided").value(1))
                .andExpect(jsonPath("$.data.problemMarketCount").value(1));
    }

    @Test
    void detailStatusRejectsInvalidValue() throws Exception {
        insertMarket(1L, "CLOSED", LocalDateTime.now().minusDays(1), "Validation", "0.00");
        insertOption(11L, 1L, "YES", "예", 1, null, null, true,
                "100.00", "0.00", "0.50000000", 0);
        insertSettlement(501L, 1L, 11L, "IN_PROGRESS");

        mockMvc.perform(get("/api/v1/admin/markets/{marketId}/settlements/{settlementId}/details", 1L, 501L)
                        .param("status", "INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    void problemMarketTypeRejectsInvalidValue() throws Exception {
        mockMvc.perform(get("/api/v1/admin/markets/problem-markets")
                        .param("type", "INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    void settlementDetailsRejectSettlementOwnedByAnotherMarket() throws Exception {
        insertMarket(1L, "CLOSED", LocalDateTime.now().minusDays(1), "Market A", "0.00");
        insertMarket(2L, "SETTLEMENT_IN_PROGRESS", LocalDateTime.now().minusDays(1), "Market B", "100.00");
        insertOption(21L, 2L, "YES", "예", 1, null, null, true,
                "100.00", "100.00", "0.50000000", 1);
        insertSettlement(502L, 2L, 21L, "IN_PROGRESS");

        mockMvc.perform(get("/api/v1/admin/markets/{marketId}/settlements/{settlementId}/details", 1L, 502L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("MARKET_INVALID_SETTLEMENT_DATA"));
    }

    @Test
    void refundDetailsRejectVoidOwnedByAnotherMarket() throws Exception {
        insertMarket(1L, "VOIDED", LocalDateTime.now().minusDays(1), "Market A", "0.00");
        insertMarket(2L, "VOIDED", LocalDateTime.now().minusDays(1), "Market B", "100.00");
        insertVoid(702L, 2L, "IN_PROGRESS");

        mockMvc.perform(get("/api/v1/admin/markets/{marketId}/refunds/{voidId}/details", 1L, 702L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("MARKET_REFUND_NOT_ALLOWED"));
    }

    @Test
    void refundProblemsCountFailedAndUnknownAsAutoRecoverable() throws Exception {
        insertMarket(1L, "VOIDED", LocalDateTime.now().minusDays(1), "Refund Failed Unknown", "200.00");
        insertOption(11L, 1L, "YES", "예", 1, null, null, false,
                "100.00", "200.00", "0.50000000", 2);
        insertPrediction(101L, 1L, 11L, 1L, "REFUND_PENDING", LocalDateTime.now());
        insertPrediction(102L, 1L, 11L, 2L, "REFUND_UNKNOWN", LocalDateTime.now());
        insertVoid(701L, 1L, "IN_PROGRESS");
        insertRefundDetail(801L, 701L, 101L, 1L, "FAILED", "REFUND_FAILED", LocalDateTime.now());
        insertRefundDetail(802L, 701L, 102L, 2L, "UNKNOWN", "UNKNOWN_RESULT", LocalDateTime.now());

        mockMvc.perform(get("/api/v1/admin/markets/problem-markets").param("type", "REFUND"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].problemType").value("REFUND"))
                .andExpect(jsonPath("$.data.content[0].problemStatus").value("FAILED"))
                .andExpect(jsonPath("$.data.content[0].failedCount").value(1))
                .andExpect(jsonPath("$.data.content[0].unknownCount").value(1))
                .andExpect(jsonPath("$.data.content[0].autoRecoverable").value(true))
                .andExpect(jsonPath("$.data.content[0].manualCheckRequired").value(false));
    }

    @Test
    void reputationUnknownProblemIsAutoRecoverable() throws Exception {
        insertMarket(1L, "SETTLED", LocalDateTime.now().minusDays(1), "Reputation Unknown", "100.00");
        insertReputationUpdate(901L, 1L, 401L, 4L, "UNKNOWN", "TIMEOUT", "unknown result");

        mockMvc.perform(get("/api/v1/admin/markets/problem-markets").param("type", "REPUTATION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].problemType").value("REPUTATION"))
                .andExpect(jsonPath("$.data.content[0].problemStatus").value("UNKNOWN"))
                .andExpect(jsonPath("$.data.content[0].failedCount").value(0))
                .andExpect(jsonPath("$.data.content[0].unknownCount").value(1))
                .andExpect(jsonPath("$.data.content[0].autoRecoverable").value(true))
                .andExpect(jsonPath("$.data.content[0].manualCheckRequired").value(false));
    }

    @Test
    void allNewEndpointsRequireAdminRole() throws Exception {
        List<String> paths = List.of(
                "/api/v1/admin/markets/1",
                "/api/v1/admin/markets/problem-markets",
                "/api/v1/admin/markets/1/settlements",
                "/api/v1/admin/markets/1/settlements/1/details",
                "/api/v1/admin/markets/1/refunds",
                "/api/v1/admin/markets/1/refunds/1/details",
                "/api/v1/admin/markets/status-counts"
        );
        for (String path : paths) {
            rawMockMvc.perform(get(path))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
            rawMockMvc.perform(get(path).header("X-Member-Role", "USER"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
        }
    }

    private void insertSettlementProblem(long marketId) {
        insertMarket(marketId, "SETTLEMENT_IN_PROGRESS", LocalDateTime.now().minusDays(1), "Settlement Problem", "100.00");
        insertOption(11L, marketId, "YES", "예", 1, null, null, true,
                "100.00", "100.00", "0.50000000", 1);
        insertPrediction(101L, marketId, 11L, 1L, "CONFIRMED", LocalDateTime.now());
        insertSettlement(501L, marketId, 11L, "IN_PROGRESS");
        insertSettlementDetail(601L, 501L, 101L, 1L, "FAILED", "SETTLEMENT_FAILED");
    }

    private void insertRefundProblem(long marketId) {
        insertMarket(marketId, "VOIDED", LocalDateTime.now().minusDays(1), "Refund Problem", "100.00");
        insertOption(21L, marketId, "YES", "예", 1, null, null, false,
                "100.00", "100.00", "0.50000000", 1);
        insertPrediction(201L, marketId, 21L, 2L, "REFUND_PENDING", LocalDateTime.now().minusMinutes(5));
        insertVoid(701L, marketId, "IN_PROGRESS");
        insertRefundDetail(801L, 701L, 201L, 2L, "PENDING", null, LocalDateTime.now().minusMinutes(5));
    }

    private void insertPredictionProblem(long marketId) {
        insertMarket(marketId, "ACTIVE", LocalDateTime.now().plusDays(1), "Prediction Problem", "0.00");
        insertOption(31L, marketId, "YES", "예", 1, null, null, false,
                "100.00", "0.00", "0.50000000", 0);
        insertPrediction(301L, marketId, 31L, 3L, "POINT_UNKNOWN", LocalDateTime.now());
    }

    private void insertReputationProblem(long marketId) {
        insertMarket(marketId, "SETTLED", LocalDateTime.now().minusDays(1), "Reputation Problem", "100.00");
        insertReputationUpdate(901L, marketId, 401L, 4L,
                "FAILED", "VALIDATION_FAILED", "manual check");
    }

    private void insertReputationUpdate(
            long id, long marketId, long predictionId, long memberId,
            String status, String lastErrorCode, String lastErrorMessage
    ) {
        jdbcTemplate.update("""
                INSERT INTO market_reputation_update (
                    id, market_id, prediction_id, member_id, is_correct, status, attempt_no,
                    last_error_code, last_error_message, created_at, updated_at
                ) VALUES (?, ?, ?, ?, TRUE, ?, 1, ?, ?, ?, ?)
                """, id, marketId, predictionId, memberId, status, lastErrorCode, lastErrorMessage,
                LocalDateTime.now(), LocalDateTime.now());
    }

    private void insertMarket(long id, String status, LocalDateTime closeAt, String title, String totalPool) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("""
                INSERT INTO market (
                    id, title, description, category, answer_type, metric_unit,
                    judge_data_source, judge_criteria, judge_date, status, close_at, settle_due_at,
                    total_pool, fee_rate, fee_amount, settlement_pool, initial_virtual_liquidity,
                    price_model, created_by, created_at, updated_at
                ) VALUES (?, ?, 'description', 'PRICE_INDEX', 'NUMERIC_RANGE', 'PERCENT',
                          'TEST_SOURCE', 'TEST_CRITERIA', ?, ?, ?, ?, ?, 5.00, 0.00, 0.00, 200.00,
                          'POOL_SHARE', 1, ?, ?)
                """,
                id, title, LocalDate.now().plusDays(1), status, closeAt, closeAt.plusDays(1), totalPool, now, now);
    }

    private void insertOption(
            long id, long marketId, String code, String text, int order,
            String rangeMin, String rangeMax, boolean result,
            String virtualPool, String realPool, String currentPrice, int predictionCount
    ) {
        jdbcTemplate.update("""
                INSERT INTO market_option (
                    id, market_id, option_code, option_text, display_order, range_min, range_max,
                    min_inclusive, max_inclusive, virtual_pool_amount, real_pool_amount,
                    total_contract_quantity, current_price, prediction_count, is_result, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, TRUE, FALSE, ?, ?, 200.00000000, ?, ?, ?, ?, ?)
                """, id, marketId, code, text, order, rangeMin, rangeMax, virtualPool, realPool,
                currentPrice, predictionCount, result, LocalDateTime.now(), LocalDateTime.now());
    }

    private void insertPrediction(
            long id, long marketId, long optionId, long memberId, String status, LocalDateTime updatedAt
    ) {
        jdbcTemplate.update("""
                INSERT INTO market_prediction (
                    id, market_id, option_id, member_id, point_amount, price_snapshot, contract_quantity,
                    status, point_spend_idempotency_key, attempt_no, created_at, updated_at
                ) VALUES (?, ?, ?, ?, 100.00, 0.50000000, 200.00000000, ?, ?, 1, ?, ?)
                """, id, marketId, optionId, memberId, status, "ADMIN_QUERY_" + id, updatedAt, updatedAt);
    }

    private void insertSettlement(long id, long marketId, long optionId, String status) {
        jdbcTemplate.update("""
                INSERT INTO market_settlement (
                    id, market_id, result_option_id, total_pool, fee_rate, fee_amount, settlement_pool,
                    winning_contract_quantity, payout_per_contract, burned_point_amount,
                    status, created_at, updated_at
                ) VALUES (?, ?, ?, 200.00, 5.00, 5.00, 195.00,
                          400.00000000, 0.23750000, 0.00, ?, ?, ?)
                """, id, marketId, optionId, status, LocalDateTime.now(), LocalDateTime.now());
    }

    private void insertSettlementDetail(
            long id, long settlementId, long predictionId, long memberId, String status, String failReason
    ) {
        jdbcTemplate.update("""
                INSERT INTO market_settlement_detail (
                    id, settlement_id, prediction_id, member_id, original_point_amount,
                    contract_quantity, payout_per_contract, settled_amount, profit_amount,
                    status, idempotency_key, fail_reason, created_at, updated_at
                ) VALUES (?, ?, ?, ?, 100.00, 200.00000000, 0.23750000, 195.00, 95.00,
                          ?, ?, ?, ?, ?)
                """, id, settlementId, predictionId, memberId, status, "SETTLEMENT_DETAIL_" + id,
                failReason, LocalDateTime.now(), LocalDateTime.now());
    }

    private void insertVoid(long id, long marketId, String refundStatus) {
        jdbcTemplate.update("""
                INSERT INTO market_void (
                    id, market_id, reason_type, reason_detail, refund_status,
                    voided_by, voided_at, created_at, updated_at
                ) VALUES (?, ?, 'DATA_UNAVAILABLE', 'data unavailable', ?, 1, ?, ?, ?)
                """, id, marketId, refundStatus, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now());
    }

    private void insertRefundDetail(
            long id, long voidId, long predictionId, long memberId, String status,
            String failReason, LocalDateTime updatedAt
    ) {
        jdbcTemplate.update("""
                INSERT INTO market_refund_detail (
                    id, market_void_id, prediction_id, member_id, refund_amount,
                    status, idempotency_key, fail_reason, created_at, updated_at
                ) VALUES (?, ?, ?, ?, 100.00, ?, ?, ?, ?, ?)
                """, id, voidId, predictionId, memberId, status, "REFUND_DETAIL_" + id,
                failReason, updatedAt, updatedAt);
    }
}
