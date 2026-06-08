package com.todongsan.marketservice.market.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.todongsan.marketservice.market.client.MemberPointClient;
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
class MarketPredictionQuoteControllerTest {

    private static final long MARKET_ID = 1L;
    private static final long SELECTED_OPTION_ID = 2L;
    private static final String QUOTE_NOTICE =
            "현재 가격은 실시간으로 변동될 수 있으며, 실제 참여 시점의 가격 기준으로 계약 수량이 확정됩니다.";

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
        jdbcTemplate.update("DELETE FROM market_void");
        jdbcTemplate.update("DELETE FROM market_settlement_detail");
        jdbcTemplate.update("DELETE FROM market_settlement");
        jdbcTemplate.update("DELETE FROM market_prediction");
        jdbcTemplate.update("DELETE FROM market_option");
        jdbcTemplate.update("DELETE FROM market");
    }

    @Test
    void quotePredictionReturnsEstimatedValuesWithoutChangingState() throws Exception {
        insertActiveMarketWithQuoteOptions();

        mockMvc.perform(quoteRequest(MARKET_ID, SELECTED_OPTION_ID, "100.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.marketId").value(MARKET_ID))
                .andExpect(jsonPath("$.data.selectedOptionId").value(SELECTED_OPTION_ID))
                .andExpect(jsonPath("$.data.pointAmount").value("100.00"))
                .andExpect(jsonPath("$.data.currentPrice").value("0.20000000"))
                .andExpect(jsonPath("$.data.estimatedContractQuantity").value("500.00000000"))
                .andExpect(jsonPath("$.data.estimatedAfterPrice").value("0.27272727"))
                .andExpect(jsonPath("$.data.priceImpactRate").value("36.36363500"))
                .andExpect(jsonPath("$.data.selectedOptionEffectivePoolBefore").value("200.00"))
                .andExpect(jsonPath("$.data.selectedOptionEffectivePoolAfter").value("300.00"))
                .andExpect(jsonPath("$.data.totalEffectivePoolBefore").value("1000.00"))
                .andExpect(jsonPath("$.data.totalEffectivePoolAfter").value("1100.00"))
                .andExpect(jsonPath("$.data.notice").value(QUOTE_NOTICE));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM market_prediction", Integer.class))
                .isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM market_price_history", Integer.class))
                .isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT total_pool FROM market WHERE id = ?", String.class, MARKET_ID))
                .isEqualTo("0.00");
        assertThat(jdbcTemplate.queryForList(
                "SELECT real_pool_amount FROM market_option ORDER BY id",
                String.class
        )).containsExactly("0.00", "0.00");
        assertThat(jdbcTemplate.queryForList(
                "SELECT current_price FROM market_option ORDER BY id",
                String.class
        )).containsExactly("0.80000000", "0.20000000");
        assertThat(jdbcTemplate.queryForList(
                "SELECT total_contract_quantity FROM market_option ORDER BY id",
                String.class
        )).containsExactly("0.00000000", "0.00000000");
        verifyNoInteractions(memberPointClient);
    }

    @Test
    void quotePredictionRejectsMissingMarket() throws Exception {
        expectQuoteError(MARKET_ID, SELECTED_OPTION_ID, "100.00", 404, "MARKET_NOT_FOUND");
    }

    @ParameterizedTest
    @ValueSource(strings = {"PENDING", "CLOSED", "DATA_PENDING", "SETTLEMENT_IN_PROGRESS", "SETTLED", "VOIDED"})
    void quotePredictionRejectsMarketStatusOtherThanActive(String marketStatus) throws Exception {
        insertMarket(MARKET_ID, marketStatus, LocalDateTime.now().plusDays(1));
        insertQuoteOptions(MARKET_ID);

        expectQuoteError(MARKET_ID, SELECTED_OPTION_ID, "100.00", 409, "MARKET_NOT_ACTIVE");
    }

    @Test
    void quotePredictionRejectsActiveMarketAfterCloseAt() throws Exception {
        insertMarket(MARKET_ID, "ACTIVE", LocalDateTime.now().minusSeconds(1));
        insertQuoteOptions(MARKET_ID);

        expectQuoteError(MARKET_ID, SELECTED_OPTION_ID, "100.00", 409, "MARKET_CLOSED");
    }

    @Test
    void quotePredictionRejectsMissingOption() throws Exception {
        insertActiveMarketWithQuoteOptions();

        expectQuoteError(MARKET_ID, 999L, "100.00", 404, "MARKET_OPTION_NOT_FOUND");
    }

    @Test
    void quotePredictionRejectsNullMarketOptionIdByValidation() throws Exception {
        insertActiveMarketWithQuoteOptions();

        mockMvc.perform(post("/api/v1/markets/{marketId}/predictions/quote", MARKET_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "marketOptionId": null,
                                  "pointAmount": "100.00"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    void quotePredictionRejectsOptionFromDifferentMarket() throws Exception {
        insertActiveMarketWithQuoteOptions();
        insertMarket(2L, "ACTIVE", LocalDateTime.now().plusDays(1));
        insertOption(3L, 2L, "OTHER", 1, "100.00", "0.00", "0.50000000");

        expectQuoteError(MARKET_ID, 3L, "100.00", 404, "MARKET_OPTION_NOT_FOUND");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "9.99", "500.01", "100.001", "abc", ""})
    void quotePredictionRejectsInvalidPointAmount(String pointAmount) throws Exception {
        insertActiveMarketWithQuoteOptions();

        expectQuoteError(MARKET_ID, SELECTED_OPTION_ID, pointAmount, 400, "MARKET_INVALID_BET_AMOUNT");
    }

    @Test
    void quotePredictionRejectsNullPointAmount() throws Exception {
        insertActiveMarketWithQuoteOptions();

        mockMvc.perform(post("/api/v1/markets/{marketId}/predictions/quote", MARKET_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "marketOptionId": 2,
                                  "pointAmount": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MARKET_INVALID_BET_AMOUNT"));
    }

    @Test
    void quotePredictionRejectsCurrentPriceLessThanOrEqualToZero() throws Exception {
        insertMarket(MARKET_ID, "ACTIVE", LocalDateTime.now().plusDays(1));
        insertOption(1L, MARKET_ID, "A", 1, "800.00", "0.00", "0.80000000");
        insertOption(SELECTED_OPTION_ID, MARKET_ID, "B", 2, "200.00", "0.00", "0.00000000");

        expectQuoteError(MARKET_ID, SELECTED_OPTION_ID, "100.00", 400, "MARKET_INVALID_OPTION");
    }

    @Test
    void quotePredictionRejectsTotalEffectivePoolLessThanOrEqualToZero() throws Exception {
        insertMarket(MARKET_ID, "ACTIVE", LocalDateTime.now().plusDays(1));
        insertOption(1L, MARKET_ID, "A", 1, "0.00", "0.00", "0.80000000");
        insertOption(SELECTED_OPTION_ID, MARKET_ID, "B", 2, "0.00", "0.00", "0.20000000");

        expectQuoteError(MARKET_ID, SELECTED_OPTION_ID, "100.00", 400, "MARKET_INVALID_OPTION");
    }

    private void insertActiveMarketWithQuoteOptions() {
        insertMarket(MARKET_ID, "ACTIVE", LocalDateTime.now().plusDays(1));
        insertQuoteOptions(MARKET_ID);
    }

    private void insertQuoteOptions(long marketId) {
        insertOption(1L, marketId, "A", 1, "800.00", "0.00", "0.80000000");
        insertOption(SELECTED_OPTION_ID, marketId, "B", 2, "200.00", "0.00", "0.20000000");
    }

    private void insertMarket(long marketId, String status, LocalDateTime closeAt) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("""
                INSERT INTO market (
                    id, title, category, answer_type, judge_data_source, judge_criteria, judge_date,
                    status, close_at, total_pool, initial_virtual_liquidity, created_by, created_at, updated_at
                ) VALUES (?, 'Quote Test Market', 'PRICE_INDEX', 'YES_NO', 'TEST', 'TEST',
                          ?, ?, ?, 0.00, 1000.00, 1, ?, ?)
                """,
                marketId,
                LocalDate.now().plusDays(2),
                status,
                closeAt,
                now,
                now
        );
    }

    private void insertOption(
            long optionId,
            long marketId,
            String optionCode,
            int displayOrder,
            String virtualPoolAmount,
            String realPoolAmount,
            String currentPrice
    ) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("""
                INSERT INTO market_option (
                    id, market_id, option_code, option_text, display_order,
                    virtual_pool_amount, real_pool_amount, total_contract_quantity,
                    current_price, prediction_count, is_result, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, 0.00000000, ?, 0, FALSE, ?, ?)
                """,
                optionId,
                marketId,
                optionCode,
                optionCode,
                displayOrder,
                virtualPoolAmount,
                realPoolAmount,
                currentPrice,
                now,
                now
        );
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder quoteRequest(
            long marketId,
            long optionId,
            String pointAmount
    ) {
        return post("/api/v1/markets/{marketId}/predictions/quote", marketId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody(optionId, pointAmount));
    }

    private void expectQuoteError(
            long marketId,
            long optionId,
            String pointAmount,
            int statusCode,
            String errorCode
    ) throws Exception {
        mockMvc.perform(quoteRequest(marketId, optionId, pointAmount))
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
