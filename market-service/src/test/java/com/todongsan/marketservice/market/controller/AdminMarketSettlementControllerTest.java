package com.todongsan.marketservice.market.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.todongsan.marketservice.market.client.MemberPointClient;
import com.todongsan.marketservice.market.client.MemberPointSettlementBatchRequest;
import com.todongsan.marketservice.market.client.MemberPointSettlementBatchResponse;
import com.todongsan.marketservice.market.client.MemberPointSettlementItemResult;
import com.todongsan.marketservice.market.client.MemberPointSettlementItemStatus;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@AutoConfigureMockMvc
class AdminMarketSettlementControllerTest {

    private static final long MARKET_ID = 100L;
    private static final long WIN_OPTION_ID = 101L;
    private static final long LOSE_OPTION_ID = 102L;

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private MemberPointClient memberPointClient;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .defaultRequest(get("/").header("X-Member-Role", "ADMIN"))
                .build();
        reset(memberPointClient);
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
    void settleMarketCompletesWinnersAndLosers() throws Exception {
        insertSettlementMarket("CLOSED", WIN_OPTION_ID, "5.00");
        insertSettlementOptions();
        jdbcTemplate.update("UPDATE market_option SET virtual_pool_amount = 999999.00 WHERE market_id = ?", MARKET_ID);
        insertPrediction(1001L, WIN_OPTION_ID, 1L, "100.00", "200.00000000", "CONFIRMED");
        insertPrediction(1002L, LOSE_OPTION_ID, 2L, "100.00", "200.00000000", "CONFIRMED");
        stubSettlementResults(Map.of());

        mockMvc.perform(post("/api/v1/admin/markets/{marketId}/settlements", MARKET_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.marketId").value(MARKET_ID))
                .andExpect(jsonPath("$.data.resultOptionId").value(WIN_OPTION_ID))
                .andExpect(jsonPath("$.data.totalPool").value("200.00"))
                .andExpect(jsonPath("$.data.feeAmount").value("5.00"))
                .andExpect(jsonPath("$.data.settlementPool").value("195.00"))
                .andExpect(jsonPath("$.data.winningContractQuantity").value("200.00000000"))
                .andExpect(jsonPath("$.data.payoutPerContract").value("0.47500000"))
                .andExpect(jsonPath("$.data.burnedPointAmount").value("0.00"))
                .andExpect(jsonPath("$.data.winnerCount").value(1))
                .andExpect(jsonPath("$.data.loserCount").value(1))
                .andExpect(jsonPath("$.data.successCount").value(1))
                .andExpect(jsonPath("$.data.failedCount").value(0))
                .andExpect(jsonPath("$.data.marketStatus").value("SETTLED"))
                .andExpect(jsonPath("$.data.settlementStatus").value("COMPLETED"));

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM market WHERE id = ?", String.class, MARKET_ID))
                .isEqualTo("SETTLED");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM market_settlement WHERE market_id = ?",
                String.class,
                MARKET_ID
        )).isEqualTo("COMPLETED");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM market_settlement_detail WHERE prediction_id = 1001",
                String.class
        )).isEqualTo("SUCCESS");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT idempotency_key FROM market_settlement_detail WHERE prediction_id = 1001",
                String.class
        )).isEqualTo("MARKET_SETTLEMENT_REWARD:market:100:prediction:1001:member:1");
        assertPrediction(1001L, "SETTLED", "195.00");
        assertPrediction(1002L, "SETTLED", "0.00");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM market_reputation_update WHERE market_id = ?",
                Integer.class,
                MARKET_ID
        )).isEqualTo(2);
        assertReputationTask(1001L, true, "PENDING", 0, null, null);
        assertReputationTask(1002L, false, "PENDING", 0, null, null);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MemberPointSettlementBatchRequest> requestCaptor =
                ArgumentCaptor.forClass(MemberPointSettlementBatchRequest.class);
        verify(memberPointClient).settleMarketRewards(keyCaptor.capture(), requestCaptor.capture());
        assertThat(keyCaptor.getValue()).isEqualTo(requestCaptor.getValue().settlementId());
        assertThat(keyCaptor.getValue())
                .startsWith("MARKET_SETTLEMENT_BATCH:market:100:settlement:")
                .endsWith(":attempt:1");
        assertThat(requestCaptor.getValue().items()).hasSize(1);
        assertThat(requestCaptor.getValue().items().get(0).amount()).isEqualByComparingTo("195.00");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM market_settlement_detail",
                Integer.class
        )).isEqualTo(1);
        assertThat(requestCaptor.getValue().items().get(0).idempotencyKey())
                .isEqualTo("MARKET_SETTLEMENT_REWARD:market:100:prediction:1001:member:1");
        assertThat(requestCaptor.getValue().items().get(0).referenceType()).isEqualTo("MARKET_PREDICTION");
        assertThat(requestCaptor.getValue().items().get(0).referenceId()).isEqualTo(1001L);
    }

    @Test
    void settleMarketCompletesWithoutWinnersAndSkipsMemberPoint() throws Exception {
        insertSettlementMarket("CLOSED", WIN_OPTION_ID, "5.00");
        insertSettlementOptions();
        insertPrediction(1001L, LOSE_OPTION_ID, 1L, "40.00", "80.00000000", "CONFIRMED");
        insertPrediction(1002L, LOSE_OPTION_ID, 2L, "60.00", "120.00000000", "CONFIRMED");

        mockMvc.perform(post("/api/v1/admin/markets/{marketId}/settlements", MARKET_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalPool").value("100.00"))
                .andExpect(jsonPath("$.data.feeAmount").value("5.00"))
                .andExpect(jsonPath("$.data.settlementPool").value("95.00"))
                .andExpect(jsonPath("$.data.winningContractQuantity").value("0.00000000"))
                .andExpect(jsonPath("$.data.payoutPerContract").value("0.00000000"))
                .andExpect(jsonPath("$.data.burnedPointAmount").value("95.00"))
                .andExpect(jsonPath("$.data.winnerCount").value(0))
                .andExpect(jsonPath("$.data.loserCount").value(2))
                .andExpect(jsonPath("$.data.marketStatus").value("SETTLED"))
                .andExpect(jsonPath("$.data.settlementStatus").value("COMPLETED"));

        verifyNoInteractions(memberPointClient);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM market_settlement_detail", Integer.class))
                .isZero();
        assertPrediction(1001L, "SETTLED", "0.00");
        assertPrediction(1002L, "SETTLED", "0.00");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM market_reputation_update WHERE market_id = ?",
                Integer.class,
                MARKET_ID
        )).isEqualTo(2);
        assertReputationTask(1001L, false, "PENDING", 0, null, null);
        assertReputationTask(1002L, false, "PENDING", 0, null, null);
    }

    @Test
    void settleMarketFloorsRewardAmountsAndBurnsRemainder() throws Exception {
        insertSettlementMarket("CLOSED", WIN_OPTION_ID, "0.00");
        insertSettlementOptions();
        insertPrediction(1001L, WIN_OPTION_ID, 1L, "10.00", "1.00000000", "CONFIRMED");
        insertPrediction(1002L, WIN_OPTION_ID, 2L, "10.00", "1.00000000", "CONFIRMED");
        insertPrediction(1003L, WIN_OPTION_ID, 3L, "10.00", "1.00000000", "CONFIRMED");
        insertPrediction(1004L, LOSE_OPTION_ID, 4L, "10.00", "1.00000000", "CONFIRMED");
        stubSettlementResults(Map.of());

        mockMvc.perform(post("/api/v1/admin/markets/{marketId}/settlements", MARKET_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalPool").value("40.00"))
                .andExpect(jsonPath("$.data.payoutPerContract").value("3.33333333"))
                .andExpect(jsonPath("$.data.burnedPointAmount").value("0.01"));

        assertThat(jdbcTemplate.queryForList(
                "SELECT settled_amount FROM market_settlement_detail ORDER BY prediction_id",
                String.class
        )).containsExactly("13.33", "13.33", "13.33");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT burned_point_amount FROM market_settlement WHERE market_id = ?",
                String.class,
                MARKET_ID
        )).isEqualTo("0.01");
        assertPrediction(1004L, "SETTLED", "0.00");
    }

    @Test
    void settleMarketReturnsPrincipalWhenThereIsNoLosingPool() throws Exception {
        insertSettlementMarket("CLOSED", WIN_OPTION_ID, "5.00");
        insertSettlementOptions();
        insertPrediction(1001L, WIN_OPTION_ID, 1L, "10.00", "20.00000000", "CONFIRMED");
        stubSettlementResults(Map.of());

        mockMvc.perform(post("/api/v1/admin/markets/{marketId}/settlements", MARKET_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalPool").value("10.00"))
                .andExpect(jsonPath("$.data.feeAmount").value("0.00"))
                .andExpect(jsonPath("$.data.settlementPool").value("10.00"))
                .andExpect(jsonPath("$.data.payoutPerContract").value("0.00000000"))
                .andExpect(jsonPath("$.data.burnedPointAmount").value("0.00"));

        assertPrediction(1001L, "SETTLED", "10.00");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT profit_amount FROM market_settlement_detail WHERE prediction_id = 1001",
                String.class
        )).isEqualTo("0.00");
    }

    @Test
    void settleMarketWeightsLosingPoolRewardByWinnerContractQuantity() throws Exception {
        insertSettlementMarket("CLOSED", WIN_OPTION_ID, "5.00");
        insertSettlementOptions();
        insertPrediction(1001L, WIN_OPTION_ID, 1L, "10.00", "20.00000000", "CONFIRMED");
        insertPrediction(1002L, WIN_OPTION_ID, 2L, "10.00", "11.11111111", "CONFIRMED");
        insertPrediction(1003L, LOSE_OPTION_ID, 3L, "100.00", "200.00000000", "CONFIRMED");
        stubSettlementResults(Map.of());

        mockMvc.perform(post("/api/v1/admin/markets/{marketId}/settlements", MARKET_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.feeAmount").value("5.00"))
                .andExpect(jsonPath("$.data.settlementPool").value("115.00"));

        List<String> settledAmounts = jdbcTemplate.queryForList(
                "SELECT settled_amount FROM market_settlement_detail ORDER BY prediction_id",
                String.class
        );
        assertThat(settledAmounts).containsExactly("71.07", "43.92");
        assertThat(new BigDecimal(settledAmounts.get(0))).isGreaterThan(new BigDecimal(settledAmounts.get(1)));
        assertPrediction(1003L, "SETTLED", "0.00");
    }

    @Test
    void settleMarketIncludesEveryNonResultOptionInLosingPool() throws Exception {
        long thirdOptionId = 103L;
        insertSettlementMarket("CLOSED", WIN_OPTION_ID, "5.00");
        insertSettlementOptions();
        insertSettlementOption(thirdOptionId, "C", false);
        insertPrediction(1001L, WIN_OPTION_ID, 1L, "20.00", "20.00000000", "CONFIRMED");
        insertPrediction(1002L, LOSE_OPTION_ID, 2L, "40.00", "40.00000000", "CONFIRMED");
        insertPrediction(1003L, thirdOptionId, 3L, "60.00", "60.00000000", "CONFIRMED");
        stubSettlementResults(Map.of());

        mockMvc.perform(post("/api/v1/admin/markets/{marketId}/settlements", MARKET_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalPool").value("120.00"))
                .andExpect(jsonPath("$.data.feeAmount").value("5.00"))
                .andExpect(jsonPath("$.data.settlementPool").value("115.00"));

        assertPrediction(1001L, "SETTLED", "115.00");
        assertPrediction(1002L, "SETTLED", "0.00");
        assertPrediction(1003L, "SETTLED", "0.00");
    }

    @Test
    void settleMarketTreatsAlreadyProcessedAsSuccess() throws Exception {
        insertSettlementMarket("CLOSED", WIN_OPTION_ID, "0.00");
        insertSettlementOptions();
        insertPrediction(1001L, WIN_OPTION_ID, 1L, "100.00", "100.00000000", "CONFIRMED");
        stubSettlementResults(Map.of(1001L, MemberPointSettlementItemStatus.ALREADY_PROCESSED));

        mockMvc.perform(post("/api/v1/admin/markets/{marketId}/settlements", MARKET_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.successCount").value(1))
                .andExpect(jsonPath("$.data.failedCount").value(0))
                .andExpect(jsonPath("$.data.marketStatus").value("SETTLED"));

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM market_settlement_detail", String.class))
                .isEqualTo("SUCCESS");
        assertPrediction(1001L, "SETTLED", "100.00");
    }

    @Test
    void settleMarketRejectsNoConfirmedPredictionsAndRollsBackMarketStatus() throws Exception {
        insertSettlementMarket("CLOSED", WIN_OPTION_ID, "5.00");
        insertSettlementOptions();

        expectSettlementError(MARKET_ID, 409, "MARKET_NO_PREDICTIONS");

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM market WHERE id = ?", String.class, MARKET_ID))
                .isEqualTo("CLOSED");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM market_settlement", Integer.class)).isZero();
        verifyNoInteractions(memberPointClient);
    }

    @Test
    void settleMarketRejectsAlreadySettledMarket() throws Exception {
        insertSettlementMarket("SETTLED", WIN_OPTION_ID, "5.00");
        insertSettlementOptions();

        expectSettlementError(MARKET_ID, 409, "MARKET_ALREADY_SETTLED");
    }

    @Test
    void settleMarketRejectsNonClosedMarket() throws Exception {
        insertSettlementMarket("ACTIVE", WIN_OPTION_ID, "5.00");
        insertSettlementOptions();

        expectSettlementError(MARKET_ID, 409, "MARKET_INVALID_STATUS");
    }

    @Test
    void settleMarketRejectsMissingResultOptionAndRollsBackMarketStatus() throws Exception {
        insertSettlementMarket("CLOSED", null, "5.00");
        insertSettlementOptions();
        insertPrediction(1001L, WIN_OPTION_ID, 1L, "100.00", "100.00000000", "CONFIRMED");

        expectSettlementError(MARKET_ID, 409, "MARKET_INVALID_SETTLEMENT_DATA");

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM market WHERE id = ?", String.class, MARKET_ID))
                .isEqualTo("CLOSED");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM market_settlement", Integer.class)).isZero();
        verifyNoInteractions(memberPointClient);
    }

    @Test
    void settleMarketKeepsInProgressWhenMemberPointItemFailed() throws Exception {
        insertSettlementMarket("CLOSED", WIN_OPTION_ID, "0.00");
        insertSettlementOptions();
        insertPrediction(1001L, WIN_OPTION_ID, 1L, "100.00", "1.00000000", "CONFIRMED");
        insertPrediction(1002L, WIN_OPTION_ID, 2L, "100.00", "2.00000000", "CONFIRMED");
        insertPrediction(1003L, LOSE_OPTION_ID, 3L, "100.00", "1.00000000", "CONFIRMED");
        stubSettlementResults(Map.of(1002L, MemberPointSettlementItemStatus.FAILED));

        mockMvc.perform(post("/api/v1/admin/markets/{marketId}/settlements", MARKET_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.successCount").value(1))
                .andExpect(jsonPath("$.data.failedCount").value(1))
                .andExpect(jsonPath("$.data.marketStatus").value("SETTLEMENT_IN_PROGRESS"))
                .andExpect(jsonPath("$.data.settlementStatus").value("IN_PROGRESS"));

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM market WHERE id = ?", String.class, MARKET_ID))
                .isEqualTo("SETTLEMENT_IN_PROGRESS");
        assertThat(jdbcTemplate.queryForList(
                "SELECT status FROM market_settlement_detail ORDER BY prediction_id",
                String.class
        )).containsExactly("SUCCESS", "FAILED");
        assertPrediction(1001L, "SETTLED", "133.33");
        assertPrediction(1002L, "CONFIRMED", null);
        assertPrediction(1003L, "SETTLED", "0.00");
    }

    @Test
    void settleMarketMarksNullItemStatusAsUnknown() throws Exception {
        insertSettlementMarket("CLOSED", WIN_OPTION_ID, "0.00");
        insertSettlementOptions();
        insertPrediction(1001L, WIN_OPTION_ID, 1L, "100.00", "1.00000000", "CONFIRMED");
        stubSettlementRawResults(List.of(new MemberPointSettlementItemResult(
                1001L,
                1L,
                null,
                null,
                new BigDecimal("100.00"),
                null
        )));

        mockMvc.perform(post("/api/v1/admin/markets/{marketId}/settlements", MARKET_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.successCount").value(0))
                .andExpect(jsonPath("$.data.failedCount").value(1))
                .andExpect(jsonPath("$.data.marketStatus").value("SETTLEMENT_IN_PROGRESS"))
                .andExpect(jsonPath("$.data.settlementStatus").value("IN_PROGRESS"));

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM market_settlement_detail", String.class))
                .isEqualTo("UNKNOWN");
        assertPrediction(1001L, "CONFIRMED", null);
    }

    @Test
    void settleMarketMarksMissingItemResultAsUnknown() throws Exception {
        insertSettlementMarket("CLOSED", WIN_OPTION_ID, "0.00");
        insertSettlementOptions();
        insertPrediction(1001L, WIN_OPTION_ID, 1L, "100.00", "1.00000000", "CONFIRMED");
        stubSettlementRawResults(List.of());

        mockMvc.perform(post("/api/v1/admin/markets/{marketId}/settlements", MARKET_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.successCount").value(0))
                .andExpect(jsonPath("$.data.failedCount").value(1))
                .andExpect(jsonPath("$.data.marketStatus").value("SETTLEMENT_IN_PROGRESS"));

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM market_settlement_detail", String.class))
                .isEqualTo("UNKNOWN");
        assertPrediction(1001L, "CONFIRMED", null);
    }

    @Test
    void settleMarketMarksUnknownOnBatchTimeout() throws Exception {
        insertSettlementMarket("CLOSED", WIN_OPTION_ID, "0.00");
        insertSettlementOptions();
        insertPrediction(1001L, WIN_OPTION_ID, 1L, "100.00", "100.00000000", "CONFIRMED");
        insertPrediction(1002L, LOSE_OPTION_ID, 2L, "100.00", "100.00000000", "CONFIRMED");
        when(memberPointClient.settleMarketRewards(
                anyString(),
                any(MemberPointSettlementBatchRequest.class)
        )).thenThrow(new MemberPointTimeoutException("timeout"));

        mockMvc.perform(post("/api/v1/admin/markets/{marketId}/settlements", MARKET_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.successCount").value(0))
                .andExpect(jsonPath("$.data.failedCount").value(1))
                .andExpect(jsonPath("$.data.marketStatus").value("SETTLEMENT_IN_PROGRESS"))
                .andExpect(jsonPath("$.data.settlementStatus").value("IN_PROGRESS"));

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM market_settlement_detail", String.class))
                .isEqualTo("UNKNOWN");
        assertPrediction(1001L, "CONFIRMED", null);
        assertPrediction(1002L, "SETTLED", "0.00");
    }

    @Test
    void retryMarketSettlementRetriesFailedDetailAndCompletesSettlement() throws Exception {
        long settlementId = 500L;
        insertSettlementMarket("SETTLEMENT_IN_PROGRESS", WIN_OPTION_ID, "0.00");
        insertSettlementOptions();
        insertPrediction(1001L, WIN_OPTION_ID, 1L, "100.00", "1.00000000", "CONFIRMED");
        insertExistingSettlement(settlementId, "IN_PROGRESS");
        insertSettlementDetail(9001L, settlementId, 1001L, 1L, "FAILED", "100.00");
        stubSettlementResults(Map.of());

        mockMvc.perform(post("/api/v1/admin/markets/{marketId}/settlements/retry", MARKET_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.successCount").value(1))
                .andExpect(jsonPath("$.data.failedCount").value(0))
                .andExpect(jsonPath("$.data.marketStatus").value("SETTLED"))
                .andExpect(jsonPath("$.data.settlementStatus").value("COMPLETED"));

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM market WHERE id = ?", String.class, MARKET_ID))
                .isEqualTo("SETTLED");
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM market_settlement_detail", String.class))
                .isEqualTo("SUCCESS");
        assertPrediction(1001L, "SETTLED", "100.00");
        assertReputationTask(1001L, true, "PENDING", 0, null, null);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MemberPointSettlementBatchRequest> requestCaptor =
                ArgumentCaptor.forClass(MemberPointSettlementBatchRequest.class);
        verify(memberPointClient).settleMarketRewards(keyCaptor.capture(), requestCaptor.capture());
        assertThat(keyCaptor.getValue()).isEqualTo(requestCaptor.getValue().settlementId());
        assertThat(keyCaptor.getValue())
                .startsWith("MARKET_SETTLEMENT_BATCH:market:100:settlement:500:retry:");
        assertThat(requestCaptor.getValue().items()).hasSize(1);
        assertThat(requestCaptor.getValue().items().get(0).reason())
                .isEqualTo("Market 정산 보상 재시도");
        assertThat(requestCaptor.getValue().items().get(0).idempotencyKey())
                .isEqualTo("RETRY_DETAIL_KEY_1001");
    }

    @Test
    void retryMarketSettlementTreatsAlreadyProcessedAsSuccess() throws Exception {
        long settlementId = 500L;
        insertSettlementMarket("SETTLEMENT_IN_PROGRESS", WIN_OPTION_ID, "0.00");
        insertSettlementOptions();
        insertPrediction(1001L, WIN_OPTION_ID, 1L, "100.00", "1.00000000", "CONFIRMED");
        insertExistingSettlement(settlementId, "IN_PROGRESS");
        insertSettlementDetail(9001L, settlementId, 1001L, 1L, "UNKNOWN", "100.00");
        stubSettlementResults(Map.of(1001L, MemberPointSettlementItemStatus.ALREADY_PROCESSED));

        mockMvc.perform(post("/api/v1/admin/markets/{marketId}/settlements/retry", MARKET_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.successCount").value(1))
                .andExpect(jsonPath("$.data.failedCount").value(0))
                .andExpect(jsonPath("$.data.marketStatus").value("SETTLED"));

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM market_settlement_detail", String.class))
                .isEqualTo("SUCCESS");
        assertPrediction(1001L, "SETTLED", "100.00");
    }

    @Test
    void retryMarketSettlementKeepsInProgressWhenOneItemStillFails() throws Exception {
        long settlementId = 500L;
        insertSettlementMarket("SETTLEMENT_IN_PROGRESS", WIN_OPTION_ID, "0.00");
        insertSettlementOptions();
        insertPrediction(1001L, WIN_OPTION_ID, 1L, "100.00", "1.00000000", "CONFIRMED");
        insertPrediction(1002L, WIN_OPTION_ID, 2L, "100.00", "1.00000000", "CONFIRMED");
        insertExistingSettlement(settlementId, "IN_PROGRESS");
        insertSettlementDetail(9001L, settlementId, 1001L, 1L, "FAILED", "100.00");
        insertSettlementDetail(9002L, settlementId, 1002L, 2L, "UNKNOWN", "100.00");
        stubSettlementResults(Map.of(1002L, MemberPointSettlementItemStatus.FAILED));

        mockMvc.perform(post("/api/v1/admin/markets/{marketId}/settlements/retry", MARKET_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.successCount").value(1))
                .andExpect(jsonPath("$.data.failedCount").value(1))
                .andExpect(jsonPath("$.data.marketStatus").value("SETTLEMENT_IN_PROGRESS"))
                .andExpect(jsonPath("$.data.settlementStatus").value("IN_PROGRESS"));

        assertThat(jdbcTemplate.queryForList(
                "SELECT status FROM market_settlement_detail ORDER BY prediction_id",
                String.class
        )).containsExactly("SUCCESS", "FAILED");
        assertPrediction(1001L, "SETTLED", "100.00");
        assertPrediction(1002L, "CONFIRMED", null);
    }

    @Test
    void retryMarketSettlementMarksNullItemStatusAsUnknown() throws Exception {
        long settlementId = 500L;
        insertSettlementMarket("SETTLEMENT_IN_PROGRESS", WIN_OPTION_ID, "0.00");
        insertSettlementOptions();
        insertPrediction(1001L, WIN_OPTION_ID, 1L, "100.00", "1.00000000", "CONFIRMED");
        insertExistingSettlement(settlementId, "IN_PROGRESS");
        insertSettlementDetail(9001L, settlementId, 1001L, 1L, "FAILED", "100.00");
        stubSettlementRawResults(List.of(new MemberPointSettlementItemResult(
                1001L,
                1L,
                null,
                null,
                new BigDecimal("100.00"),
                null
        )));

        mockMvc.perform(post("/api/v1/admin/markets/{marketId}/settlements/retry", MARKET_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.successCount").value(0))
                .andExpect(jsonPath("$.data.failedCount").value(1))
                .andExpect(jsonPath("$.data.marketStatus").value("SETTLEMENT_IN_PROGRESS"));

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM market_settlement_detail", String.class))
                .isEqualTo("UNKNOWN");
        assertPrediction(1001L, "CONFIRMED", null);
    }

    @Test
    void retryMarketSettlementMarksUnknownOnBatchTimeout() throws Exception {
        long settlementId = 500L;
        insertSettlementMarket("SETTLEMENT_IN_PROGRESS", WIN_OPTION_ID, "0.00");
        insertSettlementOptions();
        insertPrediction(1001L, WIN_OPTION_ID, 1L, "100.00", "1.00000000", "CONFIRMED");
        insertExistingSettlement(settlementId, "IN_PROGRESS");
        insertSettlementDetail(9001L, settlementId, 1001L, 1L, "FAILED", "100.00");
        when(memberPointClient.settleMarketRewards(
                anyString(),
                any(MemberPointSettlementBatchRequest.class)
        )).thenThrow(new MemberPointTimeoutException("timeout"));

        mockMvc.perform(post("/api/v1/admin/markets/{marketId}/settlements/retry", MARKET_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.successCount").value(0))
                .andExpect(jsonPath("$.data.failedCount").value(1))
                .andExpect(jsonPath("$.data.marketStatus").value("SETTLEMENT_IN_PROGRESS"));

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM market_settlement_detail", String.class))
                .isEqualTo("UNKNOWN");
        assertPrediction(1001L, "CONFIRMED", null);
    }

    @Test
    void retryMarketSettlementCompletesWhenNoRetryableDetailsAndAllSuccess() throws Exception {
        long settlementId = 500L;
        insertSettlementMarket("SETTLEMENT_IN_PROGRESS", WIN_OPTION_ID, "0.00");
        insertSettlementOptions();
        insertPrediction(1001L, WIN_OPTION_ID, 1L, "100.00", "1.00000000", "SETTLED");
        insertExistingSettlement(settlementId, "IN_PROGRESS");
        insertSettlementDetail(9001L, settlementId, 1001L, 1L, "SUCCESS", "100.00");
        insertExistingReputationTask(1001L, 1L, true, "UNKNOWN", 2, "TIMEOUT", "timeout");

        mockMvc.perform(post("/api/v1/admin/markets/{marketId}/settlements/retry", MARKET_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.successCount").value(0))
                .andExpect(jsonPath("$.data.failedCount").value(0))
                .andExpect(jsonPath("$.data.marketStatus").value("SETTLED"))
                .andExpect(jsonPath("$.data.settlementStatus").value("COMPLETED"));

        verifyNoInteractions(memberPointClient);
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM market WHERE id = ?", String.class, MARKET_ID))
                .isEqualTo("SETTLED");
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM market_settlement", String.class))
                .isEqualTo("COMPLETED");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM market_reputation_update", Integer.class))
                .isEqualTo(1);
        assertReputationTask(1001L, true, "UNKNOWN", 2, "TIMEOUT", "timeout");
    }

    @Test
    void retryMarketSettlementRejectsNoRetryableDetailsWithPendingDetail() throws Exception {
        long settlementId = 500L;
        insertSettlementMarket("SETTLEMENT_IN_PROGRESS", WIN_OPTION_ID, "0.00");
        insertSettlementOptions();
        insertPrediction(1001L, WIN_OPTION_ID, 1L, "100.00", "1.00000000", "CONFIRMED");
        insertExistingSettlement(settlementId, "IN_PROGRESS");
        insertSettlementDetail(9001L, settlementId, 1001L, 1L, "PENDING", "100.00");

        expectRetrySettlementError(MARKET_ID, 409, "MARKET_INVALID_SETTLEMENT_DATA");

        verifyNoInteractions(memberPointClient);
    }

    @Test
    void retryMarketSettlementRejectsInvalidStartStates() throws Exception {
        expectRetrySettlementError(MARKET_ID, 404, "MARKET_NOT_FOUND");

        insertSettlementMarket("SETTLED", WIN_OPTION_ID, "0.00");
        insertSettlementOptions();
        expectRetrySettlementError(MARKET_ID, 409, "MARKET_ALREADY_SETTLED");
    }

    @Test
    void retryMarketSettlementRejectsInProgressMarketWithoutSettlement() throws Exception {
        insertSettlementMarket("SETTLEMENT_IN_PROGRESS", WIN_OPTION_ID, "0.00");
        insertSettlementOptions();

        expectRetrySettlementError(MARKET_ID, 409, "MARKET_INVALID_SETTLEMENT_DATA");
    }

    private void stubSettlementResults(Map<Long, MemberPointSettlementItemStatus> statuses) {
        when(memberPointClient.settleMarketRewards(
                anyString(),
                any(MemberPointSettlementBatchRequest.class)
        )).thenAnswer(invocation -> {
            MemberPointSettlementBatchRequest request = invocation.getArgument(1);
            return new MemberPointSettlementBatchResponse(
                    request.marketId(),
                    request.items().stream()
                            .map(item -> {
                                MemberPointSettlementItemStatus status = statuses.getOrDefault(
                                        item.predictionId(),
                                        MemberPointSettlementItemStatus.PROCESSED
                                );
                                return new MemberPointSettlementItemResult(
                                        item.predictionId(),
                                        item.memberId(),
                                        status,
                                        status == MemberPointSettlementItemStatus.FAILED ? "MEMBER_NOT_FOUND" : null,
                                        item.amount(),
                                        null
                                );
                            })
                            .toList()
            );
        });
    }

    private void stubSettlementRawResults(List<MemberPointSettlementItemResult> results) {
        when(memberPointClient.settleMarketRewards(
                anyString(),
                any(MemberPointSettlementBatchRequest.class)
        )).thenReturn(new MemberPointSettlementBatchResponse(MARKET_ID, results));
    }

    private void expectSettlementError(long marketId, int statusCode, String errorCode) throws Exception {
        mockMvc.perform(post("/api/v1/admin/markets/{marketId}/settlements", marketId))
                .andExpect(status().is(statusCode))
                .andExpect(jsonPath("$.errorCode").value(errorCode));
    }

    private void expectRetrySettlementError(long marketId, int statusCode, String errorCode) throws Exception {
        mockMvc.perform(post("/api/v1/admin/markets/{marketId}/settlements/retry", marketId))
                .andExpect(status().is(statusCode))
                .andExpect(jsonPath("$.errorCode").value(errorCode));
    }

    private void insertSettlementMarket(String status, Long resultOptionId, String feeRate) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("""
                INSERT INTO market (
                    id, title, category, answer_type, judge_data_source, judge_criteria, judge_date,
                    status, close_at, result_option_id, total_pool, fee_rate, fee_amount,
                    settlement_pool, initial_virtual_liquidity, created_by, created_at, updated_at
                ) VALUES (?, 'Settlement Test Market', 'PRICE_INDEX', 'MULTIPLE_CHOICE', 'TEST', 'TEST',
                          ?, ?, ?, ?, 0.00, ?, 0.00, 0.00, 200.00, 1, ?, ?)
                """,
                MARKET_ID,
                LocalDate.now(),
                status,
                LocalDateTime.now().minusDays(1),
                resultOptionId,
                new BigDecimal(feeRate),
                now,
                now
        );
    }

    private void insertSettlementOptions() {
        insertSettlementOption(WIN_OPTION_ID, "A", true);
        insertSettlementOption(LOSE_OPTION_ID, "B", false);
    }

    private void insertSettlementOption(long optionId, String optionCode, boolean isResult) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("""
                INSERT INTO market_option (
                    id, market_id, option_code, option_text, display_order,
                    virtual_pool_amount, real_pool_amount, total_contract_quantity,
                    current_price, prediction_count, is_result, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, 100.00, 0.00, 0.00000000, 0.50000000, 0, ?, ?, ?)
                """,
                optionId,
                MARKET_ID,
                optionCode,
                optionCode,
                optionId,
                isResult,
                now,
                now
        );
    }

    private void insertPrediction(
            long predictionId,
            long optionId,
            long memberId,
            String pointAmount,
            String contractQuantity,
            String status
    ) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("""
                INSERT INTO market_prediction (
                    id, market_id, option_id, member_id, point_amount, price_snapshot, contract_quantity,
                    status, point_spend_idempotency_key, attempt_no, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, 0.50000000, ?, ?, ?, 1, ?, ?)
                """,
                predictionId,
                MARKET_ID,
                optionId,
                memberId,
                new BigDecimal(pointAmount),
                new BigDecimal(contractQuantity),
                status,
                "SETTLEMENT_TEST_KEY_" + predictionId,
                now,
                now
        );
    }

    private void insertExistingSettlement(long settlementId, String status) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("""
                INSERT INTO market_settlement (
                    id, market_id, result_option_id, total_pool, fee_rate, fee_amount,
                    settlement_pool, winning_contract_quantity, payout_per_contract,
                    burned_point_amount, status, created_at, updated_at
                ) VALUES (?, ?, ?, 100.00, 0.00, 0.00, 100.00, 1.00000000,
                          100.00000000, 0.00, ?, ?, ?)
                """,
                settlementId,
                MARKET_ID,
                WIN_OPTION_ID,
                status,
                now,
                now
        );
    }

    private void insertSettlementDetail(
            long detailId,
            long settlementId,
            long predictionId,
            long memberId,
            String status,
            String settledAmount
    ) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("""
                INSERT INTO market_settlement_detail (
                    id, settlement_id, prediction_id, member_id, original_point_amount,
                    contract_quantity, payout_per_contract, settled_amount, profit_amount,
                    status, idempotency_key, fail_reason, created_at, updated_at
                ) VALUES (?, ?, ?, ?, 100.00, 1.00000000, 100.00000000, ?, 0.00,
                          ?, ?, 'previous failure', ?, ?)
                """,
                detailId,
                settlementId,
                predictionId,
                memberId,
                new BigDecimal(settledAmount),
                status,
                "RETRY_DETAIL_KEY_" + predictionId,
                now,
                now
        );
    }

    private void assertPrediction(long predictionId, String status, String settledAmount) {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM market_prediction WHERE id = ?",
                String.class,
                predictionId
        )).isEqualTo(status);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT settled_amount FROM market_prediction WHERE id = ?",
                String.class,
                predictionId
        )).isEqualTo(settledAmount);
    }

    private void insertExistingReputationTask(
            long predictionId,
            long memberId,
            boolean isCorrect,
            String status,
            int attemptNo,
            String errorCode,
            String errorMessage
    ) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("""
                INSERT INTO market_reputation_update (
                    market_id, prediction_id, member_id, is_correct, status, attempt_no,
                    last_error_code, last_error_message, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                MARKET_ID,
                predictionId,
                memberId,
                isCorrect,
                status,
                attemptNo,
                errorCode,
                errorMessage,
                now,
                now
        );
    }

    private void assertReputationTask(
            long predictionId,
            boolean isCorrect,
            String status,
            int attemptNo,
            String errorCode,
            String errorMessage
    ) {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT is_correct FROM market_reputation_update WHERE prediction_id = ?",
                Boolean.class,
                predictionId
        )).isEqualTo(isCorrect);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM market_reputation_update WHERE prediction_id = ?",
                String.class,
                predictionId
        )).isEqualTo(status);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT attempt_no FROM market_reputation_update WHERE prediction_id = ?",
                Integer.class,
                predictionId
        )).isEqualTo(attemptNo);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT last_error_code FROM market_reputation_update WHERE prediction_id = ?",
                String.class,
                predictionId
        )).isEqualTo(errorCode);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT last_error_message FROM market_reputation_update WHERE prediction_id = ?",
                String.class,
                predictionId
        )).isEqualTo(errorMessage);
    }
}
