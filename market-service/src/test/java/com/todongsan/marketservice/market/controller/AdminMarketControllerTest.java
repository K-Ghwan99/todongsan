package com.todongsan.marketservice.market.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
class AdminMarketControllerTest {

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

    @Test
    void confirmMarketResultClosesActiveMultipleChoiceMarketWithoutChangingPredictionOrHistory() throws Exception {
        insertResultMarket(100L, "ACTIVE", "MULTIPLE_CHOICE", LocalDateTime.now().minusMinutes(1));
        insertResultOption(101L, 100L, "A", null, null, true, false, true);
        insertResultOption(102L, 100L, "B", null, null, true, false, false);
        insertTrackingPredictionAndHistory(100L, 101L);

        mockMvc.perform(patch("/api/v1/admin/markets/{marketId}/result", 100L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resultRequest(102L, "0.1834", "공공데이터 확정")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.marketId").value(100))
                .andExpect(jsonPath("$.data.resultOptionId").value(102))
                .andExpect(jsonPath("$.data.resultValue").value("0.1834"))
                .andExpect(jsonPath("$.data.resultText").value("공공데이터 확정"))
                .andExpect(jsonPath("$.data.status").value("CLOSED"));

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM market WHERE id = 100", String.class))
                .isEqualTo("CLOSED");
        assertThat(jdbcTemplate.queryForObject("SELECT result_option_id FROM market WHERE id = 100", Long.class))
                .isEqualTo(102L);
        assertThat(jdbcTemplate.queryForObject("SELECT result_value FROM market WHERE id = 100", String.class))
                .isEqualTo("0.1834");
        assertThat(jdbcTemplate.queryForObject("SELECT result_text FROM market WHERE id = 100", String.class))
                .isEqualTo("공공데이터 확정");
        assertThat(jdbcTemplate.queryForList(
                "SELECT is_result FROM market_option WHERE market_id = 100 ORDER BY id",
                Boolean.class
        )).containsExactly(false, true);
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM market_prediction", String.class))
                .isEqualTo("CONFIRMED");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM market_price_history", Integer.class))
                .isEqualTo(1);
        verifyNoInteractions(memberPointClient);
    }

    @Test
    void confirmMarketResultClosesActiveYesNoMarket() throws Exception {
        insertResultMarket(100L, "ACTIVE", "YES_NO", LocalDateTime.now().minusMinutes(1));
        insertResultOption(101L, 100L, "YES", null, null, true, false, false);
        insertResultOption(102L, 100L, "NO", null, null, true, false, false);

        expectConfirmedResult(100L, resultRequest(101L, null, null), 101L);
    }

    @Test
    void confirmMarketResultClosesDataPendingMarket() throws Exception {
        insertResultMarket(100L, "DATA_PENDING", "MULTIPLE_CHOICE", LocalDateTime.now().plusDays(1));
        insertResultOption(101L, 100L, "A", null, null, true, false, false);
        insertResultOption(102L, 100L, "B", null, null, true, false, false);

        expectConfirmedResult(100L, resultRequest(102L, null, null), 102L);
    }

    @Test
    void confirmMarketResultCalculatesNumericRangeOptionFromResultValue() throws Exception {
        insertNumericRangeResultMarket();

        expectConfirmedResult(100L, resultRequest(null, "0.3000", null), 102L);
    }

    @Test
    void confirmMarketResultRejectsPointPendingPredictionWithoutChangingData() throws Exception {
        insertResultMarket(100L, "ACTIVE", "MULTIPLE_CHOICE", LocalDateTime.now().minusMinutes(1));
        insertResultOption(101L, 100L, "A", null, null, true, false, true);
        insertResultOption(102L, 100L, "B", null, null, true, false, false);
        insertPredictionForResult(100L, 101L, "POINT_PENDING");

        expectResultConfirmationError(100L, resultRequest(102L, null, null), 409, "MARKET_INVALID_STATUS");

        assertUnresolvedPredictionResultConfirmationBlocked("POINT_PENDING");
    }

    @Test
    void confirmMarketResultRejectsPointUnknownPredictionWithoutChangingData() throws Exception {
        insertResultMarket(100L, "ACTIVE", "MULTIPLE_CHOICE", LocalDateTime.now().minusMinutes(1));
        insertResultOption(101L, 100L, "A", null, null, true, false, true);
        insertResultOption(102L, 100L, "B", null, null, true, false, false);
        insertPredictionForResult(100L, 101L, "POINT_UNKNOWN");

        expectResultConfirmationError(100L, resultRequest(102L, null, null), 409, "MARKET_INVALID_STATUS");

        assertUnresolvedPredictionResultConfirmationBlocked("POINT_UNKNOWN");
    }

    @Test
    void confirmMarketResultRejectsMissingMarket() throws Exception {
        expectResultConfirmationError(999L, resultRequest(101L, null, null), 404, "MARKET_NOT_FOUND");
    }

    @ParameterizedTest
    @ValueSource(strings = {"PENDING", "CLOSED", "SETTLEMENT_IN_PROGRESS", "SETTLED", "VOIDED"})
    void confirmMarketResultRejectsInvalidStatuses(String marketStatus) throws Exception {
        insertResultMarket(100L, marketStatus, "MULTIPLE_CHOICE", LocalDateTime.now().minusMinutes(1));
        insertResultOption(101L, 100L, "A", null, null, true, false, false);

        expectResultConfirmationError(100L, resultRequest(101L, null, null), 409, "MARKET_INVALID_STATUS");
    }

    @Test
    void confirmMarketResultRejectsActiveMarketBeforeCloseAt() throws Exception {
        insertResultMarket(100L, "ACTIVE", "MULTIPLE_CHOICE", LocalDateTime.now().plusDays(1));
        insertResultOption(101L, 100L, "A", null, null, true, false, false);

        expectResultConfirmationError(100L, resultRequest(101L, null, null), 409, "MARKET_INVALID_STATUS");
    }

    @Test
    void confirmMarketResultRejectsOptionFromDifferentMarket() throws Exception {
        insertResultMarket(100L, "ACTIVE", "MULTIPLE_CHOICE", LocalDateTime.now().minusMinutes(1));
        insertResultOption(101L, 100L, "A", null, null, true, false, false);
        insertResultMarket(200L, "ACTIVE", "MULTIPLE_CHOICE", LocalDateTime.now().minusMinutes(1));
        insertResultOption(201L, 200L, "B", null, null, true, false, false);

        expectResultConfirmationError(100L, resultRequest(201L, null, null), 404, "MARKET_OPTION_NOT_FOUND");
    }

    @Test
    void confirmMarketResultRejectsNumericRangeWithoutMatchedOption() throws Exception {
        insertNumericRangeResultMarket();

        expectResultConfirmationError(
                100L,
                resultRequest(null, "0.7000", null),
                409,
                "MARKET_WINNING_OPTION_NOT_FOUND"
        );
    }

    @Test
    void confirmMarketResultRejectsNumericRangeWithMultipleMatchedOptions() throws Exception {
        insertResultMarket(100L, "ACTIVE", "NUMERIC_RANGE", LocalDateTime.now().minusMinutes(1));
        insertResultOption(101L, 100L, "A", "0.0000", "0.3000", true, true, false);
        insertResultOption(102L, 100L, "B", "0.3000", "0.6000", true, false, false);

        expectResultConfirmationError(
                100L,
                resultRequest(null, "0.3000", null),
                409,
                "MARKET_INVALID_SETTLEMENT_DATA"
        );
    }

    @Test
    void confirmMarketResultRejectsNumericRangeWhenRequestedOptionDoesNotMatch() throws Exception {
        insertNumericRangeResultMarket();

        expectResultConfirmationError(
                100L,
                resultRequest(101L, "0.3000", null),
                409,
                "MARKET_INVALID_SETTLEMENT_DATA"
        );
    }

    @Test
    void confirmMarketResultRejectsMissingRequiredAnswerTypeValue() throws Exception {
        insertNumericRangeResultMarket();

        expectResultConfirmationError(100L, resultRequest(null, null, null), 400, "VALIDATION_FAILED");
    }

    private void insertNumericRangeResultMarket() {
        insertResultMarket(100L, "ACTIVE", "NUMERIC_RANGE", LocalDateTime.now().minusMinutes(1));
        insertResultOption(101L, 100L, "A", "0.0000", "0.3000", true, false, false);
        insertResultOption(102L, 100L, "B", "0.3000", "0.6000", true, false, false);
    }

    private void insertResultMarket(long marketId, String marketStatus, String answerType, LocalDateTime closeAt) {
        jdbcTemplate.update("""
                INSERT INTO market (
                    id, title, category, answer_type, judge_data_source, judge_criteria, judge_date,
                    status, close_at, total_pool, initial_virtual_liquidity, created_by, created_at, updated_at
                ) VALUES (?, 'Result Test Market', 'PRICE_INDEX', ?, 'TEST', 'TEST',
                          ?, ?, ?, 0.00, 200.00, 1, ?, ?)
                """,
                marketId,
                answerType,
                LocalDate.now(),
                marketStatus,
                closeAt,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private void insertResultOption(
            long optionId,
            long marketId,
            String optionCode,
            String rangeMin,
            String rangeMax,
            boolean minInclusive,
            boolean maxInclusive,
            boolean isResult
    ) {
        jdbcTemplate.update("""
                INSERT INTO market_option (
                    id, market_id, option_code, option_text, display_order,
                    range_min, range_max, min_inclusive, max_inclusive,
                    virtual_pool_amount, real_pool_amount, total_contract_quantity,
                    current_price, prediction_count, is_result, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 100.00, 0.00, 0.00000000, 0.50000000, 0, ?, ?, ?)
                """,
                optionId,
                marketId,
                optionCode,
                optionCode,
                optionId,
                rangeMin,
                rangeMax,
                minInclusive,
                maxInclusive,
                isResult,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private void insertTrackingPredictionAndHistory(long marketId, long optionId) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("""
                INSERT INTO market_prediction (
                    id, market_id, option_id, member_id, point_amount, price_snapshot, contract_quantity,
                    status, point_spend_idempotency_key, attempt_no, created_at, updated_at
                ) VALUES (1001, ?, ?, 10, 100.00, 0.50000000, 200.00000000,
                          'CONFIRMED', 'RESULT_TEST_KEY', 1, ?, ?)
                """,
                marketId,
                optionId,
                now,
                now
        );
        jdbcTemplate.update("""
                INSERT INTO market_price_history (
                    market_id, option_id, prediction_id, price_before, price_after,
                    real_pool_before, real_pool_after, contract_quantity_before, contract_quantity_after,
                    event_type, created_at, updated_at
                ) VALUES (?, ?, 1001, 0.50000000, 0.50000000,
                          0.00, 0.00, 0.00000000, 0.00000000,
                          'PREDICTION_CONFIRMED', ?, ?)
                """,
                marketId,
                optionId,
                now,
                now
        );
    }

    private void insertPredictionForResult(long marketId, long optionId, String predictionStatus) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("""
                INSERT INTO market_prediction (
                    id, market_id, option_id, member_id, point_amount,
                    status, point_spend_idempotency_key, attempt_no, created_at, updated_at
                ) VALUES (1001, ?, ?, 10, 100.00, ?, 'RESULT_TEST_KEY', 1, ?, ?)
                """,
                marketId,
                optionId,
                predictionStatus,
                now,
                now
        );
    }

    private void assertUnresolvedPredictionResultConfirmationBlocked(String predictionStatus) {
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM market WHERE id = 100", String.class))
                .isEqualTo("ACTIVE");
        assertThat(jdbcTemplate.queryForObject("SELECT result_option_id FROM market WHERE id = 100", Long.class))
                .isNull();
        assertThat(jdbcTemplate.queryForList(
                "SELECT is_result FROM market_option WHERE market_id = 100 ORDER BY id",
                Boolean.class
        )).containsExactly(true, false);
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM market_prediction", String.class))
                .isEqualTo(predictionStatus);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM market_price_history", Integer.class))
                .isZero();
        verifyNoInteractions(memberPointClient);
    }

    private void expectConfirmedResult(long marketId, String request, long resultOptionId) throws Exception {
        mockMvc.perform(patch("/api/v1/admin/markets/{marketId}/result", marketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.marketId").value(marketId))
                .andExpect(jsonPath("$.data.resultOptionId").value(resultOptionId))
                .andExpect(jsonPath("$.data.status").value("CLOSED"));
    }

    private void expectResultConfirmationError(
            long marketId,
            String request,
            int statusCode,
            String errorCode
    ) throws Exception {
        mockMvc.perform(patch("/api/v1/admin/markets/{marketId}/result", marketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().is(statusCode))
                .andExpect(jsonPath("$.errorCode").value(errorCode));
    }

    private String resultRequest(Long resultOptionId, String resultValue, String resultText) {
        return """
                {
                  "resultOptionId": %s,
                  "resultValue": %s,
                  "resultText": %s
                }
                """.formatted(
                resultOptionId == null ? "null" : resultOptionId,
                jsonString(resultValue),
                jsonString(resultText)
        );
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
