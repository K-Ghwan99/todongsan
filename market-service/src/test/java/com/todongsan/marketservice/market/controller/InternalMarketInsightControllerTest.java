package com.todongsan.marketservice.market.controller;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.todongsan.marketservice.market.client.MemberPointClient;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class InternalMarketInsightControllerTest {

    private static final long MARKET_ID = 1L;
    private static final long RESULT_OPTION_ID = 11L;
    private static final long LOSER_OPTION_ID = 12L;
    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 6, 1, 10, 0);

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
    void getInsightSummaryReturnsSettledMarketSummaryAndOptionStatistics() throws Exception {
        insertSettledMarket(MARKET_ID);
        insertInsightOptions(MARKET_ID);
        insertCompletedSettlement(MARKET_ID);
        insertSettledPrediction(100L, MARKET_ID, RESULT_OPTION_ID, 10L, "100.00", BASE_TIME.plusSeconds(2));
        insertSettledPrediction(101L, MARKET_ID, RESULT_OPTION_ID, 11L, "200.00", BASE_TIME.plusSeconds(1));
        insertSettledPrediction(102L, MARKET_ID, LOSER_OPTION_ID, 12L, "300.00", BASE_TIME.plusSeconds(3));
        insertPointPendingPrediction(103L, MARKET_ID, RESULT_OPTION_ID, 13L);

        mockMvc.perform(get("/internal/api/v1/markets/{marketId}/insight-summary", MARKET_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.market.marketId").value(MARKET_ID))
                .andExpect(jsonPath("$.data.market.status").value("SETTLED"))
                .andExpect(jsonPath("$.data.market.resultOptionId").value(RESULT_OPTION_ID))
                .andExpect(jsonPath("$.data.market.resultValue").value("0.7500"))
                .andExpect(jsonPath("$.data.market.totalPredictionCount").value(3))
                .andExpect(jsonPath("$.data.market.totalPoolAmount").value("600.00"))
                .andExpect(jsonPath("$.data.market.settlementPoolAmount").value("570.00"))
                .andExpect(jsonPath("$.data.optionStatistics[0].optionId").value(RESULT_OPTION_ID))
                .andExpect(jsonPath("$.data.optionStatistics[0].optionCode").value("A"))
                .andExpect(jsonPath("$.data.optionStatistics[0].optionLabel").value("Option A"))
                .andExpect(jsonPath("$.data.optionStatistics[0].predictionCount").value(2))
                .andExpect(jsonPath("$.data.optionStatistics[0].participantCount").value(2))
                .andExpect(jsonPath("$.data.optionStatistics[0].poolAmount").value("300.00"))
                .andExpect(jsonPath("$.data.optionStatistics[0].finalPrice").value("0.42100000"))
                .andExpect(jsonPath("$.data.optionStatistics[0].totalContractQuantity").value("712.50000000"))
                .andExpect(jsonPath("$.data.optionStatistics[0].isResult").value(true))
                .andExpect(jsonPath("$.data.optionStatistics[1].optionId").value(LOSER_OPTION_ID))
                .andExpect(jsonPath("$.data.optionStatistics[1].poolAmount").value("300.00"))
                .andExpect(jsonPath("$.data.optionStatistics[1].isResult").value(false))
                .andExpect(jsonPath("$.data.market.nickname").doesNotExist())
                .andExpect(jsonPath("$.data.optionStatistics[0].memberId").doesNotExist());

        verifyNoInteractions(memberPointClient);
    }

    @Test
    void getInsightPredictionsReturnsSettledPredictionsOrderedByParticipatedAt() throws Exception {
        insertSettledMarket(MARKET_ID);
        insertInsightOptions(MARKET_ID);
        insertCompletedSettlement(MARKET_ID);
        insertSettledPrediction(100L, MARKET_ID, RESULT_OPTION_ID, 10L, "100.00", BASE_TIME.plusSeconds(2));
        insertSettledPrediction(101L, MARKET_ID, RESULT_OPTION_ID, 11L, "200.00", BASE_TIME.plusSeconds(1));
        insertSettledPrediction(102L, MARKET_ID, LOSER_OPTION_ID, 12L, "300.00", BASE_TIME.plusSeconds(3));
        insertPointPendingPrediction(103L, MARKET_ID, RESULT_OPTION_ID, 13L);

        mockMvc.perform(get("/internal/api/v1/markets/{marketId}/insight-predictions?page=0&size=2", MARKET_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.last").value(false))
                .andExpect(jsonPath("$.data.content[0].predictionId").value(101))
                .andExpect(jsonPath("$.data.content[0].memberId").value(11))
                .andExpect(jsonPath("$.data.content[0].optionId").value(RESULT_OPTION_ID))
                .andExpect(jsonPath("$.data.content[0].optionCode").value("A"))
                .andExpect(jsonPath("$.data.content[0].optionLabel").value("Option A"))
                .andExpect(jsonPath("$.data.content[0].pointAmount").value("200.00"))
                .andExpect(jsonPath("$.data.content[0].priceSnapshot").value("0.42100000"))
                .andExpect(jsonPath("$.data.content[0].contractQuantity").value("475.05938242"))
                .andExpect(jsonPath("$.data.content[0].status").value("SETTLED"))
                .andExpect(jsonPath("$.data.content[0].isCorrect").value(true))
                .andExpect(jsonPath("$.data.content[1].predictionId").value(100))
                .andExpect(jsonPath("$.data.content[1].isCorrect").value(true))
                .andExpect(jsonPath("$.data.content[0].nickname").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].age").doesNotExist());

        verifyNoInteractions(memberPointClient);
    }

    @Test
    void getInsightPredictionsUsesDefaultPageAndSize() throws Exception {
        insertSettledMarket(MARKET_ID);
        insertInsightOptions(MARKET_ID);
        insertCompletedSettlement(MARKET_ID);
        insertSettledPrediction(100L, MARKET_ID, RESULT_OPTION_ID, 10L, "100.00", BASE_TIME);

        mockMvc.perform(get("/internal/api/v1/markets/{marketId}/insight-predictions", MARKET_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(500))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.last").value(true));
    }

    @Test
    void getInsightPredictionsRejectsSizeOverMax() throws Exception {
        mockMvc.perform(get("/internal/api/v1/markets/{marketId}/insight-predictions?size=1001", MARKET_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    void getInsightPredictionsRejectsNegativePage() throws Exception {
        mockMvc.perform(get("/internal/api/v1/markets/{marketId}/insight-predictions?page=-1", MARKET_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    void getInsightPredictionsRejectsZeroSize() throws Exception {
        mockMvc.perform(get("/internal/api/v1/markets/{marketId}/insight-predictions?size=0", MARKET_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    void getInsightPredictionsRejectsNegativeSize() throws Exception {
        mockMvc.perform(get("/internal/api/v1/markets/{marketId}/insight-predictions?size=-1", MARKET_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    void getInsightPredictionsRejectsOffsetOverflow() throws Exception {
        insertSettledMarket(MARKET_ID);
        insertInsightOptions(MARKET_ID);
        insertCompletedSettlement(MARKET_ID);
        insertSettledPrediction(100L, MARKET_ID, RESULT_OPTION_ID, 10L, "100.00", BASE_TIME);

        mockMvc.perform(get(
                        "/internal/api/v1/markets/{marketId}/insight-predictions?page=2147484&size=1000",
                        MARKET_ID
                ))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    void getInsightPredictionsReturnsSecondPage() throws Exception {
        insertSettledMarket(MARKET_ID);
        insertInsightOptions(MARKET_ID);
        insertCompletedSettlement(MARKET_ID);
        insertSettledPrediction(100L, MARKET_ID, RESULT_OPTION_ID, 10L, "100.00", BASE_TIME.plusSeconds(1));
        insertSettledPrediction(101L, MARKET_ID, LOSER_OPTION_ID, 11L, "200.00", BASE_TIME.plusSeconds(2));
        insertSettledPrediction(102L, MARKET_ID, RESULT_OPTION_ID, 12L, "300.00", BASE_TIME.plusSeconds(3));

        mockMvc.perform(get("/internal/api/v1/markets/{marketId}/insight-predictions?page=1&size=2", MARKET_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.last").value(true))
                .andExpect(jsonPath("$.data.content[0].predictionId").value(102));
    }

    @Test
    void getInsightSummaryRejectsMissingMarket() throws Exception {
        mockMvc.perform(get("/internal/api/v1/markets/{marketId}/insight-summary", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("MARKET_NOT_FOUND"));
    }

    @Test
    void getInsightPredictionsRejectsMissingMarket() throws Exception {
        mockMvc.perform(get("/internal/api/v1/markets/{marketId}/insight-predictions", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("MARKET_NOT_FOUND"));
    }

    @Test
    void getInsightSummaryRejectsMarketThatIsNotSettled() throws Exception {
        insertMarket(MARKET_ID, "CLOSED", RESULT_OPTION_ID);
        insertInsightOptions(MARKET_ID);
        insertSettledPrediction(100L, MARKET_ID, RESULT_OPTION_ID, 10L, "100.00", BASE_TIME);

        mockMvc.perform(get("/internal/api/v1/markets/{marketId}/insight-summary", MARKET_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("MARKET_INVALID_STATUS"));
    }

    @Test
    void getInsightPredictionsRejectsSettledMarketWithoutSettledPredictions() throws Exception {
        insertSettledMarket(MARKET_ID);
        insertInsightOptions(MARKET_ID);
        insertCompletedSettlement(MARKET_ID);

        mockMvc.perform(get("/internal/api/v1/markets/{marketId}/insight-predictions", MARKET_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("MARKET_NO_PREDICTIONS"));
    }

    @Test
    void internalInsightApiDoesNotExposeShortSummaryPath() throws Exception {
        mockMvc.perform(get("/internal/api/v1/markets/{marketId}/summary", MARKET_ID))
                .andExpect(status().isNotFound());
    }

    private void insertSettledMarket(long marketId) {
        insertMarket(marketId, "SETTLED", RESULT_OPTION_ID);
    }

    private void insertMarket(long marketId, String status, long resultOptionId) {
        LocalDateTime now = BASE_TIME.plusDays(1);
        jdbcTemplate.update("""
                INSERT INTO market (
                    id, title, description, category, answer_type, judge_data_source, judge_criteria, judge_date,
                    status, close_at, settled_at, result_option_id, result_value, result_text,
                    total_pool, fee_rate, fee_amount, settlement_pool, initial_virtual_liquidity,
                    created_by, created_at, updated_at
                ) VALUES (
                    ?, 'Insight Test Market', 'Insight summary source', 'PRICE_INDEX', 'NUMERIC_RANGE',
                    'TEST_SOURCE', 'TEST_CRITERIA', ?, ?, ?, ?, ?, 0.7500, 'Option A',
                    999.00, 5.00, 30.00, 570.00, 200.00, 1, ?, ?
                )
                """,
                marketId,
                LocalDate.of(2026, 6, 30),
                status,
                BASE_TIME.minusDays(1),
                now,
                resultOptionId,
                now,
                now
        );
    }

    private void insertInsightOptions(long marketId) {
        insertOption(RESULT_OPTION_ID, marketId, "A", "Option A", 1, "0.0000", "0.5000", true, false,
                "0.42100000", "712.50000000", true);
        insertOption(LOSER_OPTION_ID, marketId, "B", "Option B", 2, "0.5000", null, true, false,
                "0.57900000", "518.13471503", false);
    }

    private void insertOption(
            long optionId,
            long marketId,
            String optionCode,
            String optionText,
            int displayOrder,
            String rangeMin,
            String rangeMax,
            boolean minInclusive,
            boolean maxInclusive,
            String currentPrice,
            String totalContractQuantity,
            boolean isResult
    ) {
        jdbcTemplate.update("""
                INSERT INTO market_option (
                    id, market_id, option_code, option_text, display_order, range_min, range_max,
                    min_inclusive, max_inclusive, virtual_pool_amount, real_pool_amount,
                    total_contract_quantity, current_price, prediction_count, is_result, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1000.00, 0.00, ?, ?, 0, ?, ?, ?)
                """,
                optionId,
                marketId,
                optionCode,
                optionText,
                displayOrder,
                rangeMin == null ? null : new BigDecimal(rangeMin),
                rangeMax == null ? null : new BigDecimal(rangeMax),
                minInclusive,
                maxInclusive,
                new BigDecimal(totalContractQuantity),
                new BigDecimal(currentPrice),
                isResult,
                BASE_TIME,
                BASE_TIME
        );
    }

    private void insertCompletedSettlement(long marketId) {
        jdbcTemplate.update("""
                INSERT INTO market_settlement (
                    market_id, result_option_id, total_pool, fee_rate, fee_amount, settlement_pool,
                    winning_contract_quantity, payout_per_contract, burned_point_amount, status,
                    settled_by, settled_at, created_at, updated_at
                ) VALUES (?, ?, 600.00, 5.00, 30.00, 570.00, 712.50000000, 0.80000000,
                          0.00, 'COMPLETED', 1, ?, ?, ?)
                """,
                marketId,
                RESULT_OPTION_ID,
                BASE_TIME.plusDays(1),
                BASE_TIME.plusDays(1),
                BASE_TIME.plusDays(1)
        );
    }

    private void insertSettledPrediction(
            long predictionId,
            long marketId,
            long optionId,
            long memberId,
            String pointAmount,
            LocalDateTime createdAt
    ) {
        jdbcTemplate.update("""
                INSERT INTO market_prediction (
                    id, market_id, option_id, member_id, point_amount, price_snapshot,
                    contract_quantity, status, point_spend_idempotency_key, attempt_no,
                    settled_amount, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, 0.42100000, ?, 'SETTLED', ?, 1, 0.00, ?, ?)
                """,
                predictionId,
                marketId,
                optionId,
                memberId,
                new BigDecimal(pointAmount),
                new BigDecimal(pointAmount).divide(new BigDecimal("0.42100000"), 8, java.math.RoundingMode.HALF_UP),
                key(marketId, memberId),
                createdAt,
                createdAt
        );
    }

    private void insertPointPendingPrediction(long predictionId, long marketId, long optionId, long memberId) {
        jdbcTemplate.update("""
                INSERT INTO market_prediction (
                    id, market_id, option_id, member_id, point_amount, status,
                    point_spend_idempotency_key, attempt_no, created_at, updated_at
                ) VALUES (?, ?, ?, ?, 400.00, 'POINT_PENDING', ?, 1, ?, ?)
                """,
                predictionId,
                marketId,
                optionId,
                memberId,
                key(marketId, memberId),
                BASE_TIME,
                BASE_TIME
        );
    }

    private String key(long marketId, long memberId) {
        return "MARKET_PREDICTION_SPEND:market:%d:member:%d:attempt:1".formatted(marketId, memberId);
    }
}
