package com.todongsan.marketservice.market.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AdminMarketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM market_price_history");
        jdbcTemplate.update("DELETE FROM market_prediction");
        jdbcTemplate.update("DELETE FROM market_option");
        jdbcTemplate.update("DELETE FROM market");
    }

    @Test
    void createMarketCreatesMarketAndOptionsWithInitialPrices() throws Exception {
        mockMvc.perform(post("/api/v1/admin/markets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest(defaultOptions())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.marketId").isNumber());

        Long marketId = jdbcTemplate.queryForObject("SELECT id FROM market", Long.class);
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM market", String.class)).isEqualTo("PENDING");
        assertThat(jdbcTemplate.queryForObject("SELECT total_pool FROM market", String.class)).isEqualTo("0.00");
        assertThat(jdbcTemplate.queryForObject("SELECT initial_virtual_liquidity FROM market", String.class))
                .isEqualTo("200.00");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM market_option", Integer.class)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForList(
                "SELECT current_price FROM market_option ORDER BY display_order",
                String.class
        )).containsExactly("0.50000000", "0.50000000");
        assertThat(jdbcTemplate.queryForList(
                "SELECT virtual_pool_amount FROM market_option ORDER BY display_order",
                String.class
        )).containsExactly("100.00", "100.00");

        mockMvc.perform(get("/api/v1/markets/{marketId}", marketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.options[0].currentPrice").value("0.50000000"))
                .andExpect(jsonPath("$.data.options[0].realPoolAmount").value("0.00"))
                .andExpect(jsonPath("$.data.options[0].virtualPoolAmount").value("100.00"));
    }

    @Test
    void createMarketRejectsSingleOption() throws Exception {
        expectInvalidOptionOptions("""
                [
                  {
                    "optionCode": "A",
                    "optionText": "0.00 이상 0.30 미만",
                    "rangeMin": "0.0000",
                    "rangeMax": "0.3000"
                  }
                ]
                """);
    }

    @Test
    void createMarketRejectsDuplicatedOptionCode() throws Exception {
        expectInvalidOptionOptions("""
                [
                  {
                    "optionCode": "A",
                    "optionText": "0.00 이상 0.30 미만",
                    "rangeMin": "0.0000",
                    "rangeMax": "0.3000"
                  },
                  {
                    "optionCode": "A",
                    "optionText": "0.30 이상 0.60 미만",
                    "rangeMin": "0.3000",
                    "rangeMax": "0.6000"
                  }
                ]
                """);
    }

    @Test
    void createMarketRejectsNonPositiveVirtualPoolAmount() throws Exception {
        expectInvalidOptionOptions("""
                [
                  {
                    "optionCode": "A",
                    "optionText": "0.00 이상 0.30 미만",
                    "rangeMin": "0.0000",
                    "rangeMax": "0.3000",
                    "virtualPoolAmount": "0.00"
                  },
                  {
                    "optionCode": "B",
                    "optionText": "0.30 이상 0.60 미만",
                    "rangeMin": "0.3000",
                    "rangeMax": "0.6000"
                  }
                ]
                """);
    }

    @Test
    void createMarketRejectsNegativeVirtualPoolAmount() throws Exception {
        expectInvalidOptionOptions("""
                [
                  {
                    "optionCode": "A",
                    "optionText": "0.00 이상 0.30 미만",
                    "rangeMin": "0.0000",
                    "rangeMax": "0.3000",
                    "virtualPoolAmount": "-100.00"
                  },
                  {
                    "optionCode": "B",
                    "optionText": "0.30 이상 0.60 미만",
                    "rangeMin": "0.3000",
                    "rangeMax": "0.6000"
                  }
                ]
                """);
    }

    @Test
    void createMarketCalculatesInitialPricesFromVirtualPoolAmountRatio() throws Exception {
        expectCreated(validRequest("""
                [
                  {
                    "optionCode": "A",
                    "optionText": "0.00 이상 0.30 미만",
                    "displayOrder": 1,
                    "rangeMin": "0.0000",
                    "rangeMax": "0.3000",
                    "virtualPoolAmount": "100.00"
                  },
                  {
                    "optionCode": "B",
                    "optionText": "0.30 이상 0.60 미만",
                    "displayOrder": 2,
                    "rangeMin": "0.3000",
                    "rangeMax": "0.6000",
                    "virtualPoolAmount": "300.00"
                  }
                ]
                """));

        assertThat(jdbcTemplate.queryForObject("SELECT initial_virtual_liquidity FROM market", String.class))
                .isEqualTo("400.00");
        assertThat(jdbcTemplate.queryForList(
                "SELECT current_price FROM market_option ORDER BY display_order",
                String.class
        )).containsExactly("0.25000000", "0.75000000");
    }

    @Test
    void createMarketRejectsOverlappedNumericRanges() throws Exception {
        expectInvalidRange("""
                [
                  {
                    "optionCode": "A",
                    "optionText": "0.00 이상 0.30 이하",
                    "rangeMin": "0.0000",
                    "rangeMax": "0.3000",
                    "maxInclusive": true
                  },
                  {
                    "optionCode": "B",
                    "optionText": "0.30 이상 0.60 미만",
                    "rangeMin": "0.3000",
                    "rangeMax": "0.6000",
                    "minInclusive": true
                  }
                ]
                """);
    }

    @Test
    void createMarketRejectsEmptyNumericRange() throws Exception {
        expectInvalidRange("""
                [
                  {
                    "optionCode": "A",
                    "optionText": "빈 구간",
                    "rangeMin": "0.3000",
                    "rangeMax": "0.3000"
                  },
                  {
                    "optionCode": "B",
                    "optionText": "0.30 이상 0.60 미만",
                    "rangeMin": "0.3000",
                    "rangeMax": "0.6000"
                  }
                ]
                """);
    }

    @Test
    void createMarketCreatesNumericRangeWithOpenEnds() throws Exception {
        expectCreated(validRequest(openEndedOptions()));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM market_option", Integer.class)).isEqualTo(4);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT range_min FROM market_option WHERE option_code = 'A'",
                String.class
        )).isNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT range_max FROM market_option WHERE option_code = 'D'",
                String.class
        )).isNull();
        assertThat(jdbcTemplate.queryForList(
                "SELECT current_price FROM market_option ORDER BY display_order",
                String.class
        )).containsExactly("0.25000000", "0.25000000", "0.25000000", "0.25000000");
    }

    @Test
    void createMarketRejectsNumericRangeWithBothEndsOpen() throws Exception {
        expectInvalidRange("""
                [
                  {"optionCode": "A", "optionText": "모든 값", "rangeMin": null, "rangeMax": null},
                  {"optionCode": "B", "optionText": "0.00 이상", "rangeMin": "0.0000", "rangeMax": null}
                ]
                """);
    }

    @Test
    void createMarketRejectsMultipleOpenStartNumericRanges() throws Exception {
        expectInvalidRange("""
                [
                  {"optionCode": "A", "optionText": "0.00 미만", "rangeMin": null, "rangeMax": "0.0000"},
                  {"optionCode": "B", "optionText": "0.30 미만", "rangeMin": null, "rangeMax": "0.3000"},
                  {"optionCode": "C", "optionText": "0.30 이상", "rangeMin": "0.3000", "rangeMax": null}
                ]
                """);
    }

    @Test
    void createMarketRejectsMultipleOpenEndNumericRanges() throws Exception {
        expectInvalidRange("""
                [
                  {"optionCode": "A", "optionText": "0.00 미만", "rangeMin": null, "rangeMax": "0.0000"},
                  {"optionCode": "B", "optionText": "0.00 이상", "rangeMin": "0.0000", "rangeMax": null},
                  {"optionCode": "C", "optionText": "0.30 이상", "rangeMin": "0.3000", "rangeMax": null}
                ]
                """);
    }

    @Test
    void createMarketRejectsNumericRangeWhenBoundaryIsIncludedTwice() throws Exception {
        expectInvalidRange("""
                [
                  {"optionCode": "A", "optionText": "0.00 이하", "rangeMin": null, "rangeMax": "0.0000", "maxInclusive": true},
                  {"optionCode": "B", "optionText": "0.00 이상", "rangeMin": "0.0000", "rangeMax": "0.3000", "minInclusive": true}
                ]
                """);
    }

    @Test
    void createMarketRejectsNumericRangeWhenBoundaryIsExcludedTwice() throws Exception {
        expectInvalidRange("""
                [
                  {"optionCode": "A", "optionText": "0.00 미만", "rangeMin": null, "rangeMax": "0.0000", "maxInclusive": false},
                  {"optionCode": "B", "optionText": "0.00 초과", "rangeMin": "0.0000", "rangeMax": "0.3000", "minInclusive": false}
                ]
                """);
    }

    @Test
    void createMarketRejectsNumericRangeWithMiddleGap() throws Exception {
        expectInvalidRange("""
                [
                  {"optionCode": "A", "optionText": "0.00 미만", "rangeMin": null, "rangeMax": "0.0000"},
                  {"optionCode": "B", "optionText": "0.30 이상", "rangeMin": "0.3000", "rangeMax": "0.6000"}
                ]
                """);
    }

    @Test
    void createMarketAllowsFiniteContiguousNumericRanges() throws Exception {
        expectCreated(validRequest(defaultOptions()));
    }

    @Test
    void createMarketAllowsNegativeOpenEndedNumericRanges() throws Exception {
        expectCreated(validRequest("""
                [
                  {"optionCode": "A", "optionText": "-0.50 미만", "displayOrder": 1, "rangeMin": null, "rangeMax": "-0.5000"},
                  {"optionCode": "B", "optionText": "-0.50 이상 0.00 미만", "displayOrder": 2, "rangeMin": "-0.5000", "rangeMax": "0.0000"},
                  {"optionCode": "C", "optionText": "0.00 이상 0.50 미만", "displayOrder": 3, "rangeMin": "0.0000", "rangeMax": "0.5000"},
                  {"optionCode": "D", "optionText": "0.50 이상", "displayOrder": 4, "rangeMin": "0.5000", "rangeMax": null}
                ]
                """));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT range_max FROM market_option WHERE option_code = 'A'",
                String.class
        )).isEqualTo("-0.5000");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT range_min FROM market_option WHERE option_code = 'B'",
                String.class
        )).isEqualTo("-0.5000");
    }

    @Test
    void createMarketRejectsPastCloseAt() throws Exception {
        mockMvc.perform(post("/api/v1/admin/markets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request(LocalDateTime.now().minusDays(1), defaultOptions())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MARKET_INVALID_OPTION"));
    }

    @Test
    void createMarketRejectsNegativeFeeRate() throws Exception {
        expectInvalidFeeRate(request(
                LocalDateTime.now().plusDays(2),
                LocalDate.now().plusDays(3),
                LocalDateTime.now().plusDays(4),
                "-0.01",
                "NUMERIC_RANGE",
                defaultOptions()
        ));
    }

    @Test
    void createMarketRejectsFeeRateOverOneHundred() throws Exception {
        expectInvalidFeeRate(request(
                LocalDateTime.now().plusDays(2),
                LocalDate.now().plusDays(3),
                LocalDateTime.now().plusDays(4),
                "100.01",
                "NUMERIC_RANGE",
                defaultOptions()
        ));
    }

    @Test
    void createMarketUsesDefaultFeeRateWhenOmitted() throws Exception {
        expectCreated(request(
                LocalDateTime.now().plusDays(2),
                LocalDate.now().plusDays(3),
                LocalDateTime.now().plusDays(4),
                null,
                "NUMERIC_RANGE",
                defaultOptions()
        ));

        assertThat(jdbcTemplate.queryForObject("SELECT fee_rate FROM market", String.class)).isEqualTo("5.00");
    }

    @Test
    void createMarketRejectsJudgeDateBeforeCloseDate() throws Exception {
        LocalDateTime closeAt = LocalDateTime.now().plusDays(2);
        expectInvalidOption(request(
                closeAt,
                closeAt.toLocalDate().minusDays(1),
                closeAt.plusDays(1),
                "5.00",
                "NUMERIC_RANGE",
                defaultOptions()
        ));
    }

    @Test
    void createMarketAllowsJudgeDateEqualToCloseDate() throws Exception {
        LocalDateTime closeAt = LocalDateTime.now().plusDays(2);
        expectCreated(request(
                closeAt,
                closeAt.toLocalDate(),
                closeAt.plusDays(1),
                "5.00",
                "NUMERIC_RANGE",
                defaultOptions()
        ));
    }

    @Test
    void createMarketAllowsJudgeDateAfterCloseDate() throws Exception {
        LocalDateTime closeAt = LocalDateTime.now().plusDays(2);
        expectCreated(request(
                closeAt,
                closeAt.toLocalDate().plusDays(1),
                closeAt.plusDays(1),
                "5.00",
                "NUMERIC_RANGE",
                defaultOptions()
        ));
    }

    @Test
    void createMarketRejectsSettleDueAtBeforeCloseAt() throws Exception {
        LocalDateTime closeAt = LocalDateTime.now().plusDays(2);
        expectInvalidOption(request(
                closeAt,
                closeAt.toLocalDate(),
                closeAt.minusSeconds(1),
                "5.00",
                "NUMERIC_RANGE",
                defaultOptions()
        ));
    }

    @Test
    void createMarketRejectsSettleDueAtEqualToCloseAt() throws Exception {
        LocalDateTime closeAt = LocalDateTime.now().plusDays(2);
        expectInvalidOption(request(
                closeAt,
                closeAt.toLocalDate(),
                closeAt,
                "5.00",
                "NUMERIC_RANGE",
                defaultOptions()
        ));
    }

    @Test
    void createMarketRejectsSettleDueAtBeforeJudgeDate() throws Exception {
        LocalDateTime closeAt = LocalDateTime.now().plusDays(2);
        LocalDate judgeDate = closeAt.toLocalDate().plusDays(2);
        expectInvalidOption(request(
                closeAt,
                judgeDate,
                closeAt.plusDays(1),
                "5.00",
                "NUMERIC_RANGE",
                defaultOptions()
        ));
    }

    @Test
    void createMarketAllowsSettleDueAtAfterCloseAt() throws Exception {
        LocalDateTime closeAt = LocalDateTime.now().plusDays(2);
        expectCreated(request(
                closeAt,
                closeAt.toLocalDate(),
                closeAt.plusSeconds(1),
                "5.00",
                "NUMERIC_RANGE",
                defaultOptions()
        ));
    }

    @Test
    void createMarketAllowsNullSettleDueAt() throws Exception {
        LocalDateTime closeAt = LocalDateTime.now().plusDays(2);
        expectCreated(request(
                closeAt,
                closeAt.toLocalDate(),
                null,
                "5.00",
                "NUMERIC_RANGE",
                defaultOptions()
        ));
    }

    @Test
    void createMarketRejectsYesNoWithSingleOption() throws Exception {
        expectInvalidOption(request(
                LocalDateTime.now().plusDays(2),
                LocalDate.now().plusDays(3),
                LocalDateTime.now().plusDays(4),
                "5.00",
                "YES_NO",
                yesNoOptions(1)
        ));
    }

    @Test
    void createMarketAllowsYesNoWithTwoOptions() throws Exception {
        expectCreated(request(
                LocalDateTime.now().plusDays(2),
                LocalDate.now().plusDays(3),
                LocalDateTime.now().plusDays(4),
                "5.00",
                "YES_NO",
                yesNoOptions(2)
        ));
    }

    @Test
    void createMarketRejectsYesNoWithThreeOptions() throws Exception {
        expectInvalidOption(request(
                LocalDateTime.now().plusDays(2),
                LocalDate.now().plusDays(3),
                LocalDateTime.now().plusDays(4),
                "5.00",
                "YES_NO",
                yesNoOptions(3)
        ));
    }

    @Test
    void activateMarketChangesPendingMarketToActiveWithoutChangingPrices() throws Exception {
        Long marketId = createMarketAndGetId();
        var pricesBeforeActivation = jdbcTemplate.queryForList(
                "SELECT current_price FROM market_option WHERE market_id = ? ORDER BY display_order",
                String.class,
                marketId
        );

        mockMvc.perform(patch("/api/v1/admin/markets/{marketId}/activate", marketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.marketId").value(marketId))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM market WHERE id = ?",
                String.class,
                marketId
        )).isEqualTo("ACTIVE");
        assertThat(jdbcTemplate.queryForList(
                "SELECT current_price FROM market_option WHERE market_id = ? ORDER BY display_order",
                String.class,
                marketId
        )).containsExactlyElementsOf(pricesBeforeActivation);
    }

    @Test
    void activateMarketRejectsMissingMarket() throws Exception {
        expectActivationError(999L, status().isNotFound(), "MARKET_NOT_FOUND");
    }

    @Test
    void activateMarketRejectsAlreadyActiveMarket() throws Exception {
        Long marketId = createMarketAndGetId();
        activateMarket(marketId);

        expectActivationError(marketId, status().isConflict(), "MARKET_INVALID_STATUS");
    }

    @Test
    void activateMarketRejectsClosedMarket() throws Exception {
        Long marketId = createMarketAndGetId();
        jdbcTemplate.update("UPDATE market SET status = 'CLOSED' WHERE id = ?", marketId);

        expectActivationError(marketId, status().isConflict(), "MARKET_INVALID_STATUS");
    }

    @Test
    void activateMarketRejectsPendingMarketAfterCloseAt() throws Exception {
        Long marketId = createMarketAndGetId();
        jdbcTemplate.update("UPDATE market SET close_at = ? WHERE id = ?", LocalDateTime.now().minusDays(1), marketId);

        expectActivationError(marketId, status().isConflict(), "MARKET_CLOSED");
    }

    @Test
    void activateMarketMakesMarketVisibleInActiveList() throws Exception {
        Long marketId = createMarketAndGetId();
        activateMarket(marketId);

        mockMvc.perform(get("/api/v1/markets").param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].marketId").value(marketId))
                .andExpect(jsonPath("$.data.content[0].status").value("ACTIVE"));
    }

    @Test
    void activateMarketRejectsMarketWithSingleOption() throws Exception {
        Long marketId = createMarketAndGetId();
        jdbcTemplate.update(
                "DELETE FROM market_option WHERE market_id = ? AND option_code = 'B'",
                marketId
        );

        expectActivationError(marketId, status().isBadRequest(), "MARKET_INVALID_OPTION");
    }

    @Test
    void activateMarketRejectsMarketWithoutInitialVirtualLiquidity() throws Exception {
        Long marketId = createMarketAndGetId();
        jdbcTemplate.update("UPDATE market SET initial_virtual_liquidity = 0.00 WHERE id = ?", marketId);

        expectActivationError(marketId, status().isBadRequest(), "MARKET_INVALID_OPTION");
    }

    private Long createMarketAndGetId() throws Exception {
        expectCreated(validRequest(defaultOptions()));
        return jdbcTemplate.queryForObject("SELECT id FROM market", Long.class);
    }

    private void activateMarket(Long marketId) throws Exception {
        mockMvc.perform(patch("/api/v1/admin/markets/{marketId}/activate", marketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    private void expectActivationError(Long marketId, org.springframework.test.web.servlet.ResultMatcher status, String errorCode)
            throws Exception {
        mockMvc.perform(patch("/api/v1/admin/markets/{marketId}/activate", marketId))
                .andExpect(status)
                .andExpect(jsonPath("$.errorCode").value(errorCode));
    }

    private void expectInvalidOptionOptions(String options) throws Exception {
        mockMvc.perform(post("/api/v1/admin/markets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest(options)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MARKET_INVALID_OPTION"));
    }

    private void expectInvalidOption(String request) throws Exception {
        mockMvc.perform(post("/api/v1/admin/markets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MARKET_INVALID_OPTION"));
    }

    private void expectInvalidRange(String options) throws Exception {
        mockMvc.perform(post("/api/v1/admin/markets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest(options)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MARKET_INVALID_OPTION_RANGE"));
    }

    private void expectInvalidFeeRate(String request) throws Exception {
        mockMvc.perform(post("/api/v1/admin/markets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MARKET_INVALID_FEE_RATE"));
    }

    private void expectCreated(String request) throws Exception {
        mockMvc.perform(post("/api/v1/admin/markets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.marketId").isNumber());
    }

    private String validRequest(String options) {
        return request(LocalDateTime.now().plusDays(2), options);
    }

    private String request(LocalDateTime closeAt, String options) {
        return request(
                closeAt,
                LocalDate.now().plusDays(3),
                LocalDateTime.now().plusDays(4),
                "5.00",
                "NUMERIC_RANGE",
                options
        );
    }

    private String request(
            LocalDateTime closeAt,
            LocalDate judgeDate,
            LocalDateTime settleDueAt,
            String feeRate,
            String answerType,
            String options
    ) {
        return """
                {
                  "title": "이번 주 OO구 아파트 가격 변동률은?",
                  "description": "공공 데이터 기준으로 판정합니다.",
                  "category": "PRICE_INDEX",
                  "answerType": "%s",
                  "metricUnit": "PERCENT",
                  "judgeDataSource": "공공데이터 API",
                  "judgeCriteria": "지정된 판정일의 변동률 기준",
                  "judgeDate": "%s",
                  "closeAt": "%s",
                  "settleDueAt": %s,
                  "feeRate": %s,
                  "createdBy": 1,
                  "options": %s
                }
                """.formatted(
                answerType,
                judgeDate,
                closeAt,
                jsonString(settleDueAt),
                jsonString(feeRate),
                options
        );
    }

    private String jsonString(Object value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    private String yesNoOptions(int count) {
        String[] options = {
                """
                {
                  "optionCode": "YES",
                  "optionText": "예",
                  "displayOrder": 1
                }
                """,
                """
                {
                  "optionCode": "NO",
                  "optionText": "아니오",
                  "displayOrder": 2
                }
                """,
                """
                {
                  "optionCode": "UNKNOWN",
                  "optionText": "미정",
                  "displayOrder": 3
                }
                """
        };
        return "[%s]".formatted(String.join(",", java.util.Arrays.copyOf(options, count)));
    }

    private String defaultOptions() {
        return """
                [
                  {
                    "optionCode": "A",
                    "optionText": "0.00 이상 0.30 미만",
                    "displayOrder": 1,
                    "rangeMin": "0.0000",
                    "rangeMax": "0.3000",
                    "minInclusive": true,
                    "maxInclusive": false,
                    "virtualPoolAmount": "100.00"
                  },
                  {
                    "optionCode": "B",
                    "optionText": "0.30 이상 0.60 미만",
                    "displayOrder": 2,
                    "rangeMin": "0.3000",
                    "rangeMax": "0.6000",
                    "minInclusive": true,
                    "maxInclusive": false,
                    "virtualPoolAmount": "100.00"
                  }
                ]
                """;
    }

    private String openEndedOptions() {
        return """
                [
                  {"optionCode": "A", "optionText": "0.00 미만", "displayOrder": 1, "rangeMin": null, "rangeMax": "0.0000", "minInclusive": false, "maxInclusive": false, "virtualPoolAmount": "100.00"},
                  {"optionCode": "B", "optionText": "0.00 이상 0.30 미만", "displayOrder": 2, "rangeMin": "0.0000", "rangeMax": "0.3000", "minInclusive": true, "maxInclusive": false, "virtualPoolAmount": "100.00"},
                  {"optionCode": "C", "optionText": "0.30 이상 0.60 미만", "displayOrder": 3, "rangeMin": "0.3000", "rangeMax": "0.6000", "minInclusive": true, "maxInclusive": false, "virtualPoolAmount": "100.00"},
                  {"optionCode": "D", "optionText": "0.60 이상", "displayOrder": 4, "rangeMin": "0.6000", "rangeMax": null, "minInclusive": true, "maxInclusive": false, "virtualPoolAmount": "100.00"}
                ]
                """;
    }
}
