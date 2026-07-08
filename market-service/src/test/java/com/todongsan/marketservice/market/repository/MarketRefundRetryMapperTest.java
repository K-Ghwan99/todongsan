package com.todongsan.marketservice.market.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class MarketRefundRetryMapperTest {

    @Autowired
    private MarketMapper marketMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM market_refund_detail");
        jdbcTemplate.update("DELETE FROM market_void");
        jdbcTemplate.update("DELETE FROM market_prediction");
        jdbcTemplate.update("DELETE FROM market_option");
        jdbcTemplate.update("DELETE FROM market");
    }

    @Test
    void selectMarketIdsForRefundRetrySelectsOnlyRetryableVoidedMarkets() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oldPending = now.minusMinutes(4);
        LocalDateTime recentPending = now.minusMinutes(1);

        insertRefundRetryFixture(1L, "VOIDED", "IN_PROGRESS", "FAILED", oldPending);
        insertRefundRetryFixture(2L, "VOIDED", "IN_PROGRESS", "UNKNOWN", oldPending.plusSeconds(1));
        insertRefundRetryFixture(3L, "VOIDED", "IN_PROGRESS", "PENDING", oldPending.plusSeconds(2));
        insertRefundRetryFixture(4L, "VOIDED", "IN_PROGRESS", "PENDING", recentPending);
        insertRefundRetryFixture(5L, "VOIDED", "IN_PROGRESS", "SUCCESS", oldPending);
        insertRefundRetryFixture(6L, "VOIDED", "COMPLETED", "FAILED", oldPending);
        insertRefundRetryFixture(7L, "ACTIVE", "IN_PROGRESS", "FAILED", oldPending);
        insertRefundRetryFixture(8L, "VOIDED", "IN_PROGRESS", "FAILED", oldPending.plusSeconds(3));

        List<Long> marketIds = marketMapper.selectMarketIdsForRefundRetry(now.minusMinutes(3), 10);

        assertThat(marketIds).containsExactly(1L, 2L, 3L, 8L);

        List<Long> limitedMarketIds = marketMapper.selectMarketIdsForRefundRetry(now.minusMinutes(3), 2);

        assertThat(limitedMarketIds).containsExactly(1L, 2L);
    }

    private void insertRefundRetryFixture(
            long marketId,
            String marketStatus,
            String refundStatus,
            String detailStatus,
            LocalDateTime updatedAt
    ) {
        long optionId = marketId * 10;
        long predictionId = marketId * 100;
        long voidId = marketId * 1000;
        long detailId = marketId * 10000;
        insertMarket(marketId, marketStatus, updatedAt);
        insertOption(optionId, marketId, updatedAt);
        insertPrediction(predictionId, marketId, optionId, updatedAt);
        insertMarketVoid(voidId, marketId, refundStatus, updatedAt);
        insertRefundDetail(detailId, voidId, predictionId, detailStatus, updatedAt);
    }

    private void insertMarket(long marketId, String status, LocalDateTime now) {
        jdbcTemplate.update("""
                INSERT INTO market (
                    id, title, category, answer_type, judge_data_source, judge_criteria, judge_date,
                    status, close_at, total_pool, initial_virtual_liquidity, created_by, created_at, updated_at
                ) VALUES (?, 'Refund Retry Market', 'PRICE_INDEX', 'YES_NO', 'TEST', 'TEST',
                          ?, ?, ?, 0.00, 200.00, 1, ?, ?)
                """,
                marketId,
                LocalDate.now(),
                status,
                now.minusDays(1),
                now,
                now
        );
    }

    private void insertOption(long optionId, long marketId, LocalDateTime now) {
        jdbcTemplate.update("""
                INSERT INTO market_option (
                    id, market_id, option_code, option_text, display_order,
                    virtual_pool_amount, real_pool_amount, total_contract_quantity,
                    current_price, prediction_count, is_result, created_at, updated_at
                ) VALUES (?, ?, 'YES', '예', 1, 100.00, 0.00, 0.00000000, 0.50000000, 0, FALSE, ?, ?)
                """,
                optionId,
                marketId,
                now,
                now
        );
    }

    private void insertPrediction(long predictionId, long marketId, long optionId, LocalDateTime now) {
        jdbcTemplate.update("""
                INSERT INTO market_prediction (
                    id, market_id, option_id, member_id, point_amount, price_snapshot, contract_quantity,
                    status, point_spend_idempotency_key, attempt_no, refund_amount, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, 0.50000000, 200.00000000, 'REFUND_PENDING', ?, 1, NULL, ?, ?)
                """,
                predictionId,
                marketId,
                optionId,
                predictionId,
                new BigDecimal("100.00"),
                "REFUND_RETRY_TEST_KEY_" + predictionId,
                now,
                now
        );
    }

    private void insertMarketVoid(long voidId, long marketId, String refundStatus, LocalDateTime updatedAt) {
        jdbcTemplate.update("""
                INSERT INTO market_void (
                    id, market_id, reason_type, reason_detail, refund_status,
                    voided_by, voided_at, created_at, updated_at
                ) VALUES (?, ?, 'ADMIN_ERROR', NULL, ?, 1, ?, ?, ?)
                """,
                voidId,
                marketId,
                refundStatus,
                updatedAt,
                updatedAt,
                updatedAt
        );
    }

    private void insertRefundDetail(
            long detailId,
            long voidId,
            long predictionId,
            String status,
            LocalDateTime updatedAt
    ) {
        jdbcTemplate.update("""
                INSERT INTO market_refund_detail (
                    id, market_void_id, prediction_id, member_id, refund_amount,
                    status, idempotency_key, fail_reason, created_at, updated_at
                ) VALUES (?, ?, ?, ?, 100.00, ?, ?, NULL, ?, ?)
                """,
                detailId,
                voidId,
                predictionId,
                predictionId,
                status,
                "REFUND_RETRY_IDEMPOTENCY_KEY_" + predictionId,
                updatedAt,
                updatedAt
        );
    }
}
