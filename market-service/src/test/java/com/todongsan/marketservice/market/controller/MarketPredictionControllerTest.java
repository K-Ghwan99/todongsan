package com.todongsan.marketservice.market.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.todongsan.marketservice.market.client.MemberPointClient;
import com.todongsan.marketservice.market.client.PointSpendCommand;
import com.todongsan.marketservice.market.client.exception.MemberPointExternalException;
import com.todongsan.marketservice.market.client.exception.MemberPointTimeoutException;
import com.todongsan.marketservice.market.client.exception.PointInsufficientException;
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
    private static final LocalDateTime PREDICTION_CREATED_AT = LocalDateTime.of(2026, 6, 2, 15, 30, 0);

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
    void createPredictionMarksPredictionFailedWhenPointIsInsufficient() throws Exception {
        insertActiveMarketWithOptions();
        doThrow(new PointInsufficientException("POINT_INSUFFICIENT"))
                .when(memberPointClient)
                .spend(any(PointSpendCommand.class));

        mockMvc.perform(predictionRequest(MARKET_ID, MEMBER_ID, IDEMPOTENCY_KEY, 1L, "100.00"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("POINT_INSUFFICIENT"));

        assertPredictionStateWithoutPriceConfirmation("FAILED", "POINT_INSUFFICIENT");
    }

    @Test
    void createPredictionMarksPredictionUnknownWhenPointSpendTimesOut() throws Exception {
        insertActiveMarketWithOptions();
        doThrow(new MemberPointTimeoutException("MEMBER_POINT_TIMEOUT"))
                .when(memberPointClient)
                .spend(any(PointSpendCommand.class));

        mockMvc.perform(predictionRequest(MARKET_ID, MEMBER_ID, IDEMPOTENCY_KEY, 1L, "100.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("예측 참여 처리 상태를 확인 중입니다."))
                .andExpect(jsonPath("$.data.pointAmount").value("100.00"))
                .andExpect(jsonPath("$.data.priceSnapshot").value(nullValue()))
                .andExpect(jsonPath("$.data.contractQuantity").value(nullValue()))
                .andExpect(jsonPath("$.data.status").value("POINT_UNKNOWN"));

        assertPredictionStateWithoutPriceConfirmation("POINT_UNKNOWN", "MEMBER_POINT_TIMEOUT");
        mockMvc.perform(get("/api/v1/markets/{marketId}/predictions/me", MARKET_ID)
                        .header("X-Member-Id", MEMBER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("POINT_UNKNOWN"))
                .andExpect(jsonPath("$.data.priceSnapshot").value(nullValue()))
                .andExpect(jsonPath("$.data.contractQuantity").value(nullValue()));
    }

    @Test
    void createPredictionMarksPredictionUnknownWhenPointServiceReturnsExternalError() throws Exception {
        insertActiveMarketWithOptions();
        doThrow(new MemberPointExternalException("MEMBER_POINT_EXTERNAL_ERROR"))
                .when(memberPointClient)
                .spend(any(PointSpendCommand.class));

        mockMvc.perform(predictionRequest(MARKET_ID, MEMBER_ID, IDEMPOTENCY_KEY, 1L, "100.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("POINT_UNKNOWN"))
                .andExpect(jsonPath("$.data.priceSnapshot").value(nullValue()))
                .andExpect(jsonPath("$.data.contractQuantity").value(nullValue()));

        assertPredictionStateWithoutPriceConfirmation("POINT_UNKNOWN", "MEMBER_POINT_EXTERNAL_ERROR");
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

    @Test
    void getMyPredictionReturnsConfirmedPrediction() throws Exception {
        insertActiveMarketWithOptions();
        insertPrediction(
                100L,
                MEMBER_ID,
                "100.00",
                "0.50000000",
                "200.00000000",
                "CONFIRMED",
                PREDICTION_CREATED_AT,
                PREDICTION_CREATED_AT.plusSeconds(1)
        );

        mockMvc.perform(get("/api/v1/markets/{marketId}/predictions/me", MARKET_ID)
                        .header("X-Member-Id", MEMBER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.predictionId").value(100))
                .andExpect(jsonPath("$.data.marketId").value(MARKET_ID))
                .andExpect(jsonPath("$.data.selectedOptionId").value(1))
                .andExpect(jsonPath("$.data.pointAmount").value("100.00"))
                .andExpect(jsonPath("$.data.priceSnapshot").value("0.50000000"))
                .andExpect(jsonPath("$.data.contractQuantity").value("200.00000000"))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.createdAt").value("2026-06-02T15:30:00"))
                .andExpect(jsonPath("$.data.updatedAt").value("2026-06-02T15:30:01"));
    }

    @Test
    void getMyPredictionReturnsPointPendingWithNullCalculatedValues() throws Exception {
        insertActiveMarketWithOptions();
        insertPrediction(
                100L,
                MEMBER_ID,
                "100.00",
                null,
                null,
                "POINT_PENDING",
                PREDICTION_CREATED_AT,
                PREDICTION_CREATED_AT
        );

        mockMvc.perform(get("/api/v1/markets/{marketId}/predictions/me", MARKET_ID)
                        .header("X-Member-Id", MEMBER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.priceSnapshot").value(nullValue()))
                .andExpect(jsonPath("$.data.contractQuantity").value(nullValue()))
                .andExpect(jsonPath("$.data.status").value("POINT_PENDING"));
    }

    @Test
    void getMyPredictionRejectsMissingMemberId() throws Exception {
        insertActiveMarketWithOptions();

        mockMvc.perform(get("/api/v1/markets/{marketId}/predictions/me", MARKET_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    void getMyPredictionRejectsMissingMarket() throws Exception {
        mockMvc.perform(get("/api/v1/markets/{marketId}/predictions/me", MARKET_ID)
                        .header("X-Member-Id", MEMBER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("MARKET_NOT_FOUND"));
    }

    @Test
    void getMyPredictionRejectsMissingPrediction() throws Exception {
        insertActiveMarketWithOptions();

        expectMyPredictionNotFound(MEMBER_ID);
    }

    @Test
    void getMyPredictionDoesNotReturnAnotherMembersPrediction() throws Exception {
        insertActiveMarketWithOptions();
        insertPrediction(
                100L,
                MEMBER_ID,
                "100.00",
                "0.50000000",
                "200.00000000",
                "CONFIRMED",
                PREDICTION_CREATED_AT,
                PREDICTION_CREATED_AT.plusSeconds(1)
        );

        expectMyPredictionNotFound(99L);
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

    private void insertPrediction(
            long predictionId,
            long memberId,
            String pointAmount,
            String priceSnapshot,
            String contractQuantity,
            String predictionStatus,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        jdbcTemplate.update("""
                INSERT INTO market_prediction (
                    id, market_id, option_id, member_id, point_amount, price_snapshot, contract_quantity,
                    status, point_spend_idempotency_key, created_at, updated_at
                ) VALUES (?, ?, 1, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                predictionId,
                MARKET_ID,
                memberId,
                pointAmount,
                priceSnapshot,
                contractQuantity,
                predictionStatus,
                "MARKET_PREDICTION_SPEND:market:%d:member:%d".formatted(MARKET_ID, memberId),
                createdAt,
                updatedAt
        );
    }

    private void expectMyPredictionNotFound(long memberId) throws Exception {
        mockMvc.perform(get("/api/v1/markets/{marketId}/predictions/me", MARKET_ID)
                        .header("X-Member-Id", memberId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("MARKET_PREDICTION_NOT_FOUND"));
    }

    private void assertPredictionStateWithoutPriceConfirmation(String predictionStatus, String failReason) {
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM market_prediction", String.class))
                .isEqualTo(predictionStatus);
        assertThat(jdbcTemplate.queryForObject("SELECT fail_reason FROM market_prediction", String.class))
                .isEqualTo(failReason);
        assertThat(jdbcTemplate.queryForObject("SELECT price_snapshot FROM market_prediction", String.class))
                .isNull();
        assertThat(jdbcTemplate.queryForObject("SELECT contract_quantity FROM market_prediction", String.class))
                .isNull();
        assertThat(jdbcTemplate.queryForObject("SELECT total_pool FROM market", String.class)).isEqualTo("0.00");
        assertThat(jdbcTemplate.queryForList(
                "SELECT real_pool_amount FROM market_option ORDER BY id",
                String.class
        )).containsExactly("0.00", "0.00");
        assertThat(jdbcTemplate.queryForList(
                "SELECT total_contract_quantity FROM market_option ORDER BY id",
                String.class
        )).containsExactly("0.00000000", "0.00000000");
        assertThat(jdbcTemplate.queryForList(
                "SELECT current_price FROM market_option ORDER BY id",
                String.class
        )).containsExactly("0.50000000", "0.50000000");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM market_price_history", Integer.class))
                .isZero();
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
