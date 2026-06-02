package com.todongsan.marketservice.market.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.todongsan.marketservice.market.client.MemberPointClient;
import com.todongsan.marketservice.market.client.PointSpendCommand;
import com.todongsan.marketservice.market.dto.request.CreatePredictionRequest;
import com.todongsan.marketservice.market.entity.MarketPrediction;
import com.todongsan.marketservice.market.service.MarketPredictionTransactionService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class MarketPredictionControllerTest {

    private static final long MARKET_ID = 1L;
    private static final long MEMBER_ID = 10L;
    private static final String IDEMPOTENCY_KEY = "MARKET_PREDICTION_SPEND:market:1:member:10";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MarketPredictionTransactionService transactionService;

    @MockitoBean
    private MemberPointClient memberPointClient;

    @BeforeEach
    void setUp() {
        reset(memberPointClient);
        jdbcTemplate.update("DELETE FROM market_price_history");
        jdbcTemplate.update("DELETE FROM market_prediction");
        jdbcTemplate.update("DELETE FROM market_option");
        jdbcTemplate.update("DELETE FROM market");
    }

    @Test
    void createPredictionConfirmsPredictionAndRecalculatesPrices() throws Exception {
        insertActiveMarketWithOptions();

        mockMvc.perform(predictionRequest(MARKET_ID, MEMBER_ID, IDEMPOTENCY_KEY, 1L, "100.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.predictionId").isNumber())
                .andExpect(jsonPath("$.data.marketId").value(MARKET_ID))
                .andExpect(jsonPath("$.data.selectedOptionId").value(1))
                .andExpect(jsonPath("$.data.pointAmount").value("100.00"))
                .andExpect(jsonPath("$.data.priceSnapshot").value("0.50000000"))
                .andExpect(jsonPath("$.data.contractQuantity").value("200.00000000"))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));

        Long predictionId = jdbcTemplate.queryForObject("SELECT id FROM market_prediction", Long.class);
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM market_prediction", String.class))
                .isEqualTo("CONFIRMED");
        assertThat(jdbcTemplate.queryForObject("SELECT point_amount FROM market_prediction", String.class))
                .isEqualTo("100.00");
        assertThat(jdbcTemplate.queryForObject("SELECT price_snapshot FROM market_prediction", String.class))
                .isEqualTo("0.50000000");
        assertThat(jdbcTemplate.queryForObject("SELECT contract_quantity FROM market_prediction", String.class))
                .isEqualTo("200.00000000");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT point_spend_idempotency_key FROM market_prediction",
                String.class
        )).isEqualTo(IDEMPOTENCY_KEY);
        assertThat(jdbcTemplate.queryForObject("SELECT total_pool FROM market", String.class)).isEqualTo("100.00");
        assertThat(jdbcTemplate.queryForList(
                "SELECT real_pool_amount FROM market_option ORDER BY id",
                String.class
        )).containsExactly("100.00", "0.00");
        assertThat(jdbcTemplate.queryForList(
                "SELECT total_contract_quantity FROM market_option ORDER BY id",
                String.class
        )).containsExactly("200.00000000", "0.00000000");
        assertThat(jdbcTemplate.queryForList(
                "SELECT current_price FROM market_option ORDER BY id",
                String.class
        )).containsExactly("0.66666667", "0.33333333");
        assertThat(jdbcTemplate.queryForList(
                "SELECT prediction_count FROM market_option ORDER BY id",
                Integer.class
        )).containsExactly(1, 0);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM market_price_history", Integer.class))
                .isEqualTo(2);
        assertThat(jdbcTemplate.queryForList(
                "SELECT prediction_id FROM market_price_history ORDER BY option_id",
                Long.class
        )).containsExactly(predictionId, predictionId);

        ArgumentCaptor<PointSpendCommand> commandCaptor = ArgumentCaptor.forClass(PointSpendCommand.class);
        verify(memberPointClient).spend(commandCaptor.capture());
        assertThat(commandCaptor.getValue()).isEqualTo(new PointSpendCommand(
                MEMBER_ID,
                "SPEND_MARKET",
                new BigDecimal("100.00"),
                "MARKET_PREDICTION",
                predictionId,
                IDEMPOTENCY_KEY
        ));
    }

    @Test
    void createPendingPredictionCommitsPointPendingBeforePointSpend() {
        insertActiveMarketWithOptions();
        CreatePredictionRequest request = new CreatePredictionRequest();
        request.setMarketOptionId(1L);
        request.setPointAmount(new BigDecimal("100.00"));

        MarketPrediction prediction = transactionService.createPendingPrediction(
                MARKET_ID,
                MEMBER_ID,
                IDEMPOTENCY_KEY,
                request
        );

        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM market_prediction WHERE id = ?",
                String.class,
                prediction.getId()
        )).isEqualTo("POINT_PENDING");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT price_snapshot FROM market_prediction WHERE id = ?",
                String.class,
                prediction.getId()
        )).isNull();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM market_price_history", Integer.class))
                .isZero();
    }

    @Test
    void createPredictionRejectsPendingMarket() throws Exception {
        insertMarket(MARKET_ID, "PENDING", LocalDateTime.now().plusDays(1));
        insertOptions(MARKET_ID);

        expectPredictionError(MARKET_ID, MEMBER_ID, IDEMPOTENCY_KEY, 1L, "100.00", 409, "MARKET_NOT_ACTIVE");
    }

    @Test
    void createPredictionRejectsClosedMarket() throws Exception {
        insertMarket(MARKET_ID, "CLOSED", LocalDateTime.now().plusDays(1));
        insertOptions(MARKET_ID);

        expectPredictionError(MARKET_ID, MEMBER_ID, IDEMPOTENCY_KEY, 1L, "100.00", 409, "MARKET_NOT_ACTIVE");
    }

    @Test
    void createPredictionRejectsActiveMarketAfterCloseAt() throws Exception {
        insertMarket(MARKET_ID, "ACTIVE", LocalDateTime.now().minusDays(1));
        insertOptions(MARKET_ID);

        expectPredictionError(MARKET_ID, MEMBER_ID, IDEMPOTENCY_KEY, 1L, "100.00", 409, "MARKET_CLOSED");
    }

    @Test
    void createPredictionRejectsMissingMarket() throws Exception {
        expectPredictionError(MARKET_ID, MEMBER_ID, IDEMPOTENCY_KEY, 1L, "100.00", 404, "MARKET_NOT_FOUND");
    }

    @Test
    void createPredictionRejectsOptionFromDifferentMarket() throws Exception {
        insertActiveMarketWithOptions();
        insertMarket(2L, "ACTIVE", LocalDateTime.now().plusDays(1));
        insertOption(3L, 2L, "OTHER", 1);

        expectPredictionError(MARKET_ID, MEMBER_ID, IDEMPOTENCY_KEY, 3L, "100.00", 404, "MARKET_OPTION_NOT_FOUND");
    }

    @Test
    void createPredictionRejectsPointAmountBelowMinimum() throws Exception {
        insertActiveMarketWithOptions();

        expectPredictionError(MARKET_ID, MEMBER_ID, IDEMPOTENCY_KEY, 1L, "9.99", 400, "MARKET_INVALID_BET_AMOUNT");
    }

    @Test
    void createPredictionRejectsPointAmountAboveMaximum() throws Exception {
        insertActiveMarketWithOptions();

        expectPredictionError(MARKET_ID, MEMBER_ID, IDEMPOTENCY_KEY, 1L, "500.01", 400, "MARKET_INVALID_BET_AMOUNT");
    }

    @Test
    void createPredictionRejectsPointAmountWithMoreThanTwoDecimalPlaces() throws Exception {
        insertActiveMarketWithOptions();

        expectPredictionError(MARKET_ID, MEMBER_ID, IDEMPOTENCY_KEY, 1L, "100.001", 400, "MARKET_INVALID_BET_AMOUNT");
    }

    @Test
    void createPredictionRejectsDuplicatedMemberForSameMarket() throws Exception {
        insertActiveMarketWithOptions();
        mockMvc.perform(predictionRequest(MARKET_ID, MEMBER_ID, IDEMPOTENCY_KEY, 1L, "100.00"))
                .andExpect(status().isOk());

        expectPredictionError(MARKET_ID, MEMBER_ID, IDEMPOTENCY_KEY, 2L, "100.00", 409, "MARKET_ALREADY_PREDICTED");
    }

    @Test
    void createPredictionRejectsMissingIdempotencyKey() throws Exception {
        insertActiveMarketWithOptions();

        mockMvc.perform(post("/api/v1/markets/{marketId}/predictions", MARKET_ID)
                        .header("X-Member-Id", MEMBER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(1L, "100.00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    void createPredictionRejectsMissingMemberId() throws Exception {
        insertActiveMarketWithOptions();

        mockMvc.perform(post("/api/v1/markets/{marketId}/predictions", MARKET_ID)
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(1L, "100.00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    void createPredictionRejectsIdempotencyKeyWithDifferentMarketId() throws Exception {
        insertActiveMarketWithOptions();

        expectPredictionError(
                MARKET_ID,
                MEMBER_ID,
                "MARKET_PREDICTION_SPEND:market:999:member:10",
                1L,
                "100.00",
                400,
                "VALIDATION_FAILED"
        );
    }

    @Test
    void createPredictionRejectsIdempotencyKeyWithDifferentMemberId() throws Exception {
        insertActiveMarketWithOptions();

        expectPredictionError(
                MARKET_ID,
                MEMBER_ID,
                "MARKET_PREDICTION_SPEND:market:1:member:999",
                1L,
                "100.00",
                400,
                "VALIDATION_FAILED"
        );
    }

    @Test
    void createPredictionRejectsMalformedIdempotencyKey() throws Exception {
        insertActiveMarketWithOptions();

        expectPredictionError(MARKET_ID, MEMBER_ID, "WRONG_KEY", 1L, "100.00", 400, "VALIDATION_FAILED");
    }

    private void insertActiveMarketWithOptions() {
        insertMarket(MARKET_ID, "ACTIVE", LocalDateTime.now().plusDays(1));
        insertOptions(MARKET_ID);
    }

    private void insertMarket(long marketId, String status, LocalDateTime closeAt) {
        jdbcTemplate.update("""
                INSERT INTO market (
                    id, title, category, answer_type, judge_data_source, judge_criteria, judge_date,
                    status, close_at, total_pool, initial_virtual_liquidity, created_by, created_at, updated_at
                ) VALUES (?, 'Prediction Test Market', 'PRICE_INDEX', 'YES_NO', 'TEST', 'TEST',
                          ?, ?, ?, 0.00, 200.00, 1, ?, ?)
                """,
                marketId,
                LocalDate.now().plusDays(2),
                status,
                closeAt,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private void insertOptions(long marketId) {
        insertOption(1L, marketId, "A", 1);
        insertOption(2L, marketId, "B", 2);
    }

    private void insertOption(long optionId, long marketId, String optionCode, int displayOrder) {
        jdbcTemplate.update("""
                INSERT INTO market_option (
                    id, market_id, option_code, option_text, display_order,
                    virtual_pool_amount, real_pool_amount, total_contract_quantity,
                    current_price, prediction_count, is_result, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, 100.00, 0.00, 0.00000000, 0.50000000, 0, FALSE, ?, ?)
                """,
                optionId,
                marketId,
                optionCode,
                optionCode,
                displayOrder,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder predictionRequest(
            long marketId,
            long memberId,
            String idempotencyKey,
            long optionId,
            String pointAmount
    ) {
        return post("/api/v1/markets/{marketId}/predictions", marketId)
                .header("X-Member-Id", memberId)
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody(optionId, pointAmount));
    }

    private void expectPredictionError(
            long marketId,
            long memberId,
            String idempotencyKey,
            long optionId,
            String pointAmount,
            int statusCode,
            String errorCode
    ) throws Exception {
        mockMvc.perform(predictionRequest(marketId, memberId, idempotencyKey, optionId, pointAmount))
                .andExpect(status().is(statusCode))
                .andExpect(jsonPath("$.errorCode").value(errorCode));
    }

    private String requestBody(long optionId, String pointAmount) {
        return """
                {
                  "marketOptionId": %d,
                  "pointAmount": "%s"
                }
                """.formatted(optionId, pointAmount);
    }

}
