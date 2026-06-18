package com.todongsan.marketservice.market.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.todongsan.marketservice.market.client.MemberPointClient;
import com.todongsan.marketservice.market.client.MemberPointSettlementBatchRequest;
import com.todongsan.marketservice.market.client.MemberPointSettlementBatchResponse;
import com.todongsan.marketservice.market.client.MemberPointSettlementItemResult;
import com.todongsan.marketservice.market.client.MemberPointSettlementItemStatus;
import com.todongsan.marketservice.market.dto.response.SettleMarketResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class MarketSettlementIdempotencyExperimentTest {

    private static final long MARKET_ID = 2100L;
    private static final long WIN_OPTION_ID = 2101L;
    private static final long LOSE_OPTION_ID = 2102L;
    private static final long FAILED_ON_FIRST_TRY_PREDICTION_ID = 3003L;

    @Autowired
    private MarketSettlementService settlementService;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private MemberPointClient memberPointClient;

    private final List<MemberPointSettlementBatchRequest> capturedRequests = new ArrayList<>();

    @BeforeEach
    void setUp() {
        reset(memberPointClient);
        capturedRequests.clear();
        deleteAll();
    }

    @Test
    @DisplayName("[실험2] 정산 부분 실패 후 재시도 item 멱등성")
    void settlementRetryDoesNotPayAlreadySuccessfulItemsAgain() {
        seedSettlementMarket();
        seedOptions();
        insertPrediction(3001L, WIN_OPTION_ID, 501L);
        insertPrediction(3002L, WIN_OPTION_ID, 502L);
        insertPrediction(3003L, WIN_OPTION_ID, 503L);
        insertPrediction(3004L, LOSE_OPTION_ID, 504L);
        stubPartialFailureThenRetrySuccess();

        SettleMarketResponse first = settlementService.settleMarket(MARKET_ID);
        int firstSuccess = countSettlementDetailsByStatus("SUCCESS");
        int firstFailed = countSettlementDetailsByStatus("FAILED");

        SettleMarketResponse retry = settlementService.retryMarketSettlement(MARKET_ID);
        int settledPredictions = countPredictionsByStatus("SETTLED");
        int duplicatePaymentCount = countDuplicateSuccessfulItemsInRetry();

        System.out.println("[실험2] 멱등성: 전체 4 / 1차 성공 " + firstSuccess
                + " / 1차 실패 " + firstFailed
                + " / 재시도 후 SETTLED " + settledPredictions
                + " / 중복지급 " + duplicatePaymentCount);

        assertThat(first.marketStatus().name()).isEqualTo("SETTLEMENT_IN_PROGRESS");
        assertThat(retry.marketStatus().name()).isEqualTo("SETTLED");
        assertThat(firstSuccess).isEqualTo(2);
        assertThat(firstFailed).isEqualTo(1);
        assertThat(settledPredictions).isEqualTo(4);
        assertThat(countSettlementDetailsByStatus("SUCCESS")).isEqualTo(3);
        assertThat(duplicatePaymentCount).isZero();
        assertThat(capturedRequests).hasSize(2);
        assertThat(capturedRequests.get(0).items()).hasSize(3);
        assertThat(capturedRequests.get(1).items())
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.predictionId()).isEqualTo(FAILED_ON_FIRST_TRY_PREDICTION_ID);
                    assertThat(item.idempotencyKey())
                            .isEqualTo("MARKET_SETTLEMENT_REWARD:market:2100:prediction:3003:member:503");
                });
    }

    private void stubPartialFailureThenRetrySuccess() {
        AtomicInteger callCount = new AtomicInteger();
        when(memberPointClient.settleMarketRewards(anyString(), any(MemberPointSettlementBatchRequest.class)))
                .thenAnswer(invocation -> {
                    MemberPointSettlementBatchRequest request = invocation.getArgument(1);
                    capturedRequests.add(request);
                    boolean firstCall = callCount.incrementAndGet() == 1;
                    return new MemberPointSettlementBatchResponse(
                            request.marketId(),
                            request.items().stream()
                                    .map(item -> {
                                        MemberPointSettlementItemStatus status = firstCall
                                                && item.predictionId().equals(FAILED_ON_FIRST_TRY_PREDICTION_ID)
                                                ? MemberPointSettlementItemStatus.FAILED
                                                : MemberPointSettlementItemStatus.PROCESSED;
                                        return new MemberPointSettlementItemResult(
                                                item.predictionId(),
                                                item.memberId(),
                                                status,
                                                status == MemberPointSettlementItemStatus.FAILED ? "INJECTED_FAILURE" : null,
                                                item.amount(),
                                                null
                                        );
                                    })
                                    .toList()
                    );
                });
    }

    private int countDuplicateSuccessfulItemsInRetry() {
        if (capturedRequests.size() < 2) {
            return -1;
        }
        List<Long> retryPredictionIds = capturedRequests.get(1).items().stream()
                .map(item -> item.predictionId())
                .toList();
        return (int) capturedRequests.get(0).items().stream()
                .filter(item -> !item.predictionId().equals(FAILED_ON_FIRST_TRY_PREDICTION_ID))
                .filter(item -> retryPredictionIds.contains(item.predictionId()))
                .count();
    }

    private void seedSettlementMarket() {
        LocalDateTime now = LocalDateTime.now();
        jdbc.update("""
                INSERT INTO market (
                    id, title, category, answer_type, judge_data_source, judge_criteria, judge_date,
                    status, close_at, result_option_id, total_pool, fee_rate, fee_amount,
                    settlement_pool, initial_virtual_liquidity, created_by, created_at, updated_at
                ) VALUES (?, 'Experiment Settlement Market', 'PRICE_INDEX', 'MULTIPLE_CHOICE', 'TEST', 'TEST',
                          ?, 'CLOSED', ?, ?, 0.00, 0.00, 0.00, 0.00, 200.00, 1, ?, ?)
                """,
                MARKET_ID,
                LocalDate.now(),
                LocalDateTime.now().minusDays(1),
                WIN_OPTION_ID,
                now,
                now
        );
    }

    private void seedOptions() {
        insertOption(WIN_OPTION_ID, "A", true);
        insertOption(LOSE_OPTION_ID, "B", false);
    }

    private void insertOption(long optionId, String optionCode, boolean isResult) {
        LocalDateTime now = LocalDateTime.now();
        jdbc.update("""
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

    private void insertPrediction(long predictionId, long optionId, long memberId) {
        LocalDateTime now = LocalDateTime.now();
        jdbc.update("""
                INSERT INTO market_prediction (
                    id, market_id, option_id, member_id, point_amount, price_snapshot, contract_quantity,
                    status, point_spend_idempotency_key, attempt_no, created_at, updated_at
                ) VALUES (?, ?, ?, ?, 100.00, 0.50000000, 100.00000000,
                          'CONFIRMED', ?, 1, ?, ?)
                """,
                predictionId,
                MARKET_ID,
                optionId,
                memberId,
                "EXPERIMENT_SETTLEMENT_KEY_" + predictionId,
                now,
                now
        );
    }

    private int countSettlementDetailsByStatus(String status) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM market_settlement_detail WHERE status = ?",
                Integer.class,
                status
        );
    }

    private int countPredictionsByStatus(String status) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM market_prediction WHERE market_id = ? AND status = ?",
                Integer.class,
                MARKET_ID,
                status
        );
    }

    private void deleteAll() {
        jdbc.update("DELETE FROM market_price_history");
        jdbc.update("DELETE FROM market_reputation_update");
        jdbc.update("DELETE FROM market_refund_detail");
        jdbc.update("DELETE FROM market_settlement_detail");
        jdbc.update("DELETE FROM market_settlement");
        jdbc.update("DELETE FROM market_void");
        jdbc.update("DELETE FROM market_prediction");
        jdbc.update("DELETE FROM market_option");
        jdbc.update("DELETE FROM market");
    }
}
