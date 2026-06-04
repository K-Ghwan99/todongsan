package com.todongsan.marketservice.market.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class MarketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM market_price_history");
        jdbcTemplate.update("DELETE FROM market_void");
        jdbcTemplate.update("DELETE FROM market_prediction");
        jdbcTemplate.update("DELETE FROM market_option");
        jdbcTemplate.update("DELETE FROM market");

        jdbcTemplate.update("""
                INSERT INTO market (
                    id, title, description, category, answer_type, judge_data_source,
                    judge_criteria, judge_date, status, close_at, settle_due_at,
                    total_pool, created_by, created_at, updated_at
                ) VALUES (
                    1, '이번 주 OO구 아파트 가격 변동률은?', '한국부동산원 데이터를 기준으로 정산합니다.',
                    'PRICE_INDEX', 'NUMERIC_RANGE', '한국부동산원', '공식 데이터 기준',
                    '2026-06-04',
                    'ACTIVE', '2026-06-01 18:00:00', '2026-06-04 18:00:00',
                    25000.00, 1, '2026-05-29 15:00:00', '2026-05-29 15:00:00'
                )
                """);
        jdbcTemplate.update("""
                INSERT INTO market_option (
                    id, market_id, option_code, option_text, display_order,
                    virtual_pool_amount, real_pool_amount, total_contract_quantity,
                    current_price, prediction_count, is_result, created_at, updated_at
                ) VALUES
                    (1, 1, 'LOW', '0.0% 미만', 1, 5000.00, 10000.00, 10.00000000,
                     0.31250000, 1, FALSE, '2026-05-29 15:00:00', '2026-05-29 15:00:00'),
                    (2, 1, 'HIGH', '0.0% 이상 ~ 0.3% 미만', 2, 5000.00, 15000.00, 20.00000000,
                     0.68750000, 2, FALSE, '2026-05-29 15:00:00', '2026-05-29 15:00:00')
                """);
        jdbcTemplate.update("""
                INSERT INTO market_price_history (
                    id, market_id, option_id, price_before, price_after,
                    real_pool_before, real_pool_after, contract_quantity_before,
                    contract_quantity_after, event_type, created_at, updated_at
                ) VALUES
                    (1, 1, 1, 0.25000000, 0.31250000, 8000.00, 10000.00,
                     8.00000000, 10.00000000, 'PREDICTION_CONFIRMED',
                     '2026-05-29 15:30:00', '2026-05-29 15:30:00'),
                    (2, 1, 2, 0.60000000, 0.68750000, 12000.00, 15000.00,
                     16.00000000, 20.00000000, 'PREDICTION_CONFIRMED',
                     '2026-05-29 16:30:00', '2026-05-29 16:30:00')
                """);
    }

    @Test
    void getMarketsReturnsPagedSummaryWithoutDetailOnlyPoolFields() throws Exception {
        mockMvc.perform(get("/api/v1/markets")
                        .param("page", "0")
                        .param("size", "20")
                        .param("status", "ACTIVE")
                        .param("keyword", "아파트"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].marketId").value(1))
                .andExpect(jsonPath("$.data.content[0].totalPoolAmount").value("25000.00"))
                .andExpect(jsonPath("$.data.content[0].options[0].currentPrice").value("0.31250000"))
                .andExpect(jsonPath("$.data.content[0].options[0].realPoolAmount").doesNotExist())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.last").value(true));
    }

    @Test
    void getMarketReturnsDetail() throws Exception {
        mockMvc.perform(get("/api/v1/markets/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.marketId").value(1))
                .andExpect(jsonPath("$.data.resultAnnounceAt").value("2026-06-04T18:00:00"))
                .andExpect(jsonPath("$.data.options[0].realPoolAmount").value("10000.00"))
                .andExpect(jsonPath("$.data.options[0].virtualPoolAmount").value("5000.00"));
    }

    @Test
    void getMarketReturnsMarketNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/markets/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("MARKET_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Market을 찾을 수 없습니다."));
    }

    @Test
    void getPriceHistoryReturnsPagedAndFilteredHistory() throws Exception {
        mockMvc.perform(get("/api/v1/markets/1/price-history")
                        .param("page", "0")
                        .param("size", "50")
                        .param("optionId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].historyId").value(1))
                .andExpect(jsonPath("$.data.content[0].price").value("0.31250000"))
                .andExpect(jsonPath("$.data.content[0].realPoolAmount").value("10000.00"))
                .andExpect(jsonPath("$.data.content[0].virtualPoolAmount").value("5000.00"))
                .andExpect(jsonPath("$.data.content[0].contractQuantity").value("10.00000000"));
    }

    @Test
    void getPriceHistoryExcludesHistoryRowWhenOptionBelongsToDifferentMarket() throws Exception {
        jdbcTemplate.update("""
                INSERT INTO market (
                    id, title, description, category, answer_type, judge_data_source,
                    judge_criteria, judge_date, status, close_at, settle_due_at,
                    total_pool, created_by, created_at, updated_at
                ) VALUES (
                    2, '다른 Market', NULL, 'PRICE_INDEX', 'YES_NO', '한국부동산원',
                    '공식 데이터 기준', '2026-06-04', 'ACTIVE', '2026-06-01 18:00:00', NULL,
                    0.00, 1, '2026-05-29 15:00:00', '2026-05-29 15:00:00'
                )
                """);
        jdbcTemplate.update("""
                INSERT INTO market_option (
                    id, market_id, option_code, option_text, display_order,
                    virtual_pool_amount, real_pool_amount, total_contract_quantity,
                    current_price, prediction_count, is_result, created_at, updated_at
                ) VALUES (
                    3, 2, 'OTHER', '다른 Market 선택지', 1,
                    9999.00, 0.00, 0.00000000, 0.50000000, 0, FALSE,
                    '2026-05-29 15:00:00', '2026-05-29 15:00:00'
                )
                """);
        jdbcTemplate.update("""
                INSERT INTO market_price_history (
                    id, market_id, option_id, price_before, price_after,
                    real_pool_before, real_pool_after, contract_quantity_before,
                    contract_quantity_after, event_type, created_at, updated_at
                ) VALUES (
                    3, 1, 3, 0.10000000, 0.20000000, 0.00, 0.00,
                    0.00000000, 0.00000000, 'PREDICTION_CONFIRMED',
                    '2026-05-29 17:30:00', '2026-05-29 17:30:00'
                )
                """);

        mockMvc.perform(get("/api/v1/markets/1/price-history")
                        .param("page", "0")
                        .param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[0].historyId").value(2))
                .andExpect(jsonPath("$.data.content[1].historyId").value(1));
    }

    @Test
    void getMarketsRejectsInvalidPage() throws Exception {
        mockMvc.perform(get("/api/v1/markets").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    void getMarketsRejectsInvalidStatusType() throws Exception {
        mockMvc.perform(get("/api/v1/markets").param("status", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("METHOD_ARGUMENT_TYPE_MISMATCH"));
    }

    @Test
    void actuatorHealthReturnsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
