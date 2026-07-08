package com.todongsan.marketservice.market.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.todongsan.marketservice.market.type.ReputationUpdateStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class MarketReputationUpdateMapperTest {

    @Autowired
    private MarketMapper marketMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
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
    void insertReputationUpdateTaskInsertsPendingTask() {
        LocalDateTime now = LocalDateTime.now();
        MarketReputationUpdateRow row = row(1L, 100L, 10L, true, ReputationUpdateStatus.PENDING, now);

        int insertedRows = marketMapper.insertReputationUpdateTask(row);

        assertThat(insertedRows).isEqualTo(1);
        MarketReputationUpdateRow saved = marketMapper.selectReputationUpdateByPredictionId(100L);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getMarketId()).isEqualTo(1L);
        assertThat(saved.getPredictionId()).isEqualTo(100L);
        assertThat(saved.getMemberId()).isEqualTo(10L);
        assertThat(saved.getIsCorrect()).isTrue();
        assertThat(saved.getStatus()).isEqualTo(ReputationUpdateStatus.PENDING);
        assertThat(saved.getAttemptNo()).isZero();
        assertThat(saved.getLastErrorCode()).isNull();
        assertThat(saved.getLastErrorMessage()).isNull();
    }

    @Test
    void insertReputationUpdateTaskIgnoresDuplicatedPredictionId() {
        LocalDateTime now = LocalDateTime.now();
        marketMapper.insertReputationUpdateTask(row(1L, 100L, 10L, true, ReputationUpdateStatus.PENDING, now));
        jdbcTemplate.update("""
                UPDATE market_reputation_update
                SET status = 'UNKNOWN',
                    attempt_no = 2,
                    last_error_code = 'TIMEOUT',
                    last_error_message = 'timeout'
                WHERE prediction_id = 100
                """);

        int insertedRows = marketMapper.insertReputationUpdateTask(
                row(2L, 100L, 20L, false, ReputationUpdateStatus.PENDING, now.plusSeconds(1))
        );

        assertThat(insertedRows).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM market_reputation_update", Integer.class))
                .isEqualTo(1);
        MarketReputationUpdateRow saved = marketMapper.selectReputationUpdateByPredictionId(100L);
        assertThat(saved.getMarketId()).isEqualTo(1L);
        assertThat(saved.getMemberId()).isEqualTo(10L);
        assertThat(saved.getIsCorrect()).isTrue();
        assertThat(saved.getStatus()).isEqualTo(ReputationUpdateStatus.UNKNOWN);
        assertThat(saved.getAttemptNo()).isEqualTo(2);
        assertThat(saved.getLastErrorCode()).isEqualTo("TIMEOUT");
        assertThat(saved.getLastErrorMessage()).isEqualTo("timeout");
    }

    @Test
    void selectReputationUpdateByPredictionIdReturnsTask() {
        LocalDateTime now = LocalDateTime.now();
        marketMapper.insertReputationUpdateTask(row(1L, 100L, 10L, false, ReputationUpdateStatus.PENDING, now));

        MarketReputationUpdateRow saved = marketMapper.selectReputationUpdateByPredictionId(100L);

        assertThat(saved).isNotNull();
        assertThat(saved.getPredictionId()).isEqualTo(100L);
        assertThat(saved.getIsCorrect()).isFalse();
    }

    @Test
    void selectPendingOrUnknownReputationUpdatesReturnsOnlyPendingAndUnknownOrderedByUpdatedAtAndId() {
        LocalDateTime now = LocalDateTime.now();
        insertRaw(1L, 1L, 101L, 11L, "PENDING", now.plusSeconds(3));
        insertRaw(2L, 1L, 102L, 12L, "UNKNOWN", now.plusSeconds(1));
        insertRaw(3L, 1L, 103L, 13L, "FAILED", now);
        insertRaw(4L, 1L, 104L, 14L, "SUCCESS", now);
        insertRaw(5L, 1L, 105L, 15L, "PENDING", now.plusSeconds(1));
        insertRaw(6L, 1L, 106L, 16L, "UNKNOWN", now.plusSeconds(2));

        List<MarketReputationUpdateRow> rows = marketMapper.selectPendingOrUnknownReputationUpdates(10);

        assertThat(rows)
                .extracting(MarketReputationUpdateRow::getId)
                .containsExactly(2L, 5L, 6L, 1L);

        List<MarketReputationUpdateRow> limitedRows = marketMapper.selectPendingOrUnknownReputationUpdates(2);
        assertThat(limitedRows)
                .extracting(MarketReputationUpdateRow::getId)
                .containsExactly(2L, 5L);
    }

    @Test
    void markReputationUpdateSuccessClearsErrorAndIncrementsAttemptNo() {
        LocalDateTime now = LocalDateTime.now();
        insertRaw(1L, 1L, 101L, 11L, "UNKNOWN", now.minusMinutes(1));
        jdbcTemplate.update("""
                UPDATE market_reputation_update
                SET attempt_no = 2,
                    last_error_code = 'TIMEOUT',
                    last_error_message = 'timeout'
                WHERE id = 1
                """);

        int updatedRows = marketMapper.markReputationUpdateSuccess(1L, now);

        assertThat(updatedRows).isEqualTo(1);
        MarketReputationUpdateRow saved = marketMapper.selectReputationUpdateByPredictionId(101L);
        assertThat(saved.getStatus()).isEqualTo(ReputationUpdateStatus.SUCCESS);
        assertThat(saved.getAttemptNo()).isEqualTo(3);
        assertThat(saved.getLastErrorCode()).isNull();
        assertThat(saved.getLastErrorMessage()).isNull();
    }

    @Test
    void markReputationUpdateFailedStoresErrorAndIncrementsAttemptNo() {
        LocalDateTime now = LocalDateTime.now();
        insertRaw(1L, 1L, 101L, 11L, "PENDING", now.minusMinutes(1));

        int updatedRows = marketMapper.markReputationUpdateFailed(1L, "VALIDATION_FAILED", "invalid request", now);

        assertThat(updatedRows).isEqualTo(1);
        MarketReputationUpdateRow saved = marketMapper.selectReputationUpdateByPredictionId(101L);
        assertThat(saved.getStatus()).isEqualTo(ReputationUpdateStatus.FAILED);
        assertThat(saved.getAttemptNo()).isEqualTo(1);
        assertThat(saved.getLastErrorCode()).isEqualTo("VALIDATION_FAILED");
        assertThat(saved.getLastErrorMessage()).isEqualTo("invalid request");
    }

    @Test
    void markReputationUpdateUnknownStoresErrorAndIncrementsAttemptNo() {
        LocalDateTime now = LocalDateTime.now();
        insertRaw(1L, 1L, 101L, 11L, "PENDING", now.minusMinutes(1));

        int updatedRows = marketMapper.markReputationUpdateUnknown(1L, "TIMEOUT", "request timeout", now);

        assertThat(updatedRows).isEqualTo(1);
        MarketReputationUpdateRow saved = marketMapper.selectReputationUpdateByPredictionId(101L);
        assertThat(saved.getStatus()).isEqualTo(ReputationUpdateStatus.UNKNOWN);
        assertThat(saved.getAttemptNo()).isEqualTo(1);
        assertThat(saved.getLastErrorCode()).isEqualTo("TIMEOUT");
        assertThat(saved.getLastErrorMessage()).isEqualTo("request timeout");
    }

    @Test
    void insertReputationUpdateTasksForSettledPredictionsCreatesTasksOnlyForSettledPredictions() {
        LocalDateTime now = LocalDateTime.now();
        insertMarket(1L, 11L, now);
        insertOption(11L, 1L, now);
        insertOption(12L, 1L, now);
        insertPrediction(101L, 1L, 11L, 10L, "SETTLED", now);
        insertPrediction(102L, 1L, 12L, 20L, "SETTLED", now);
        insertPrediction(103L, 1L, 11L, 30L, "CONFIRMED", now);
        insertPrediction(104L, 1L, 12L, 40L, "FAILED", now);

        int insertedRows = marketMapper.insertReputationUpdateTasksForSettledPredictions(1L, now.plusSeconds(1));

        assertThat(insertedRows).isEqualTo(2);
        assertTask(101L, true, ReputationUpdateStatus.PENDING, 0, null, null);
        assertTask(102L, false, ReputationUpdateStatus.PENDING, 0, null, null);
        assertThat(marketMapper.selectReputationUpdateByPredictionId(103L)).isNull();
        assertThat(marketMapper.selectReputationUpdateByPredictionId(104L)).isNull();
    }

    @Test
    void insertReputationUpdateTasksForSettledPredictionsIgnoresExistingTasksWithoutChangingThem() {
        LocalDateTime now = LocalDateTime.now();
        insertMarket(1L, 11L, now);
        insertOption(11L, 1L, now);
        insertOption(12L, 1L, now);
        insertPrediction(101L, 1L, 11L, 10L, "SETTLED", now);
        insertPrediction(102L, 1L, 12L, 20L, "SETTLED", now);
        marketMapper.insertReputationUpdateTask(row(1L, 101L, 10L, true, ReputationUpdateStatus.PENDING, now));
        jdbcTemplate.update("""
                UPDATE market_reputation_update
                SET status = 'UNKNOWN',
                    attempt_no = 2,
                    last_error_code = 'TIMEOUT',
                    last_error_message = 'timeout'
                WHERE prediction_id = 101
                """);

        int firstInsertedRows = marketMapper.insertReputationUpdateTasksForSettledPredictions(1L, now.plusSeconds(1));
        int secondInsertedRows = marketMapper.insertReputationUpdateTasksForSettledPredictions(1L, now.plusSeconds(2));

        assertThat(firstInsertedRows).isEqualTo(1);
        assertThat(secondInsertedRows).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM market_reputation_update", Integer.class))
                .isEqualTo(2);
        assertTask(101L, true, ReputationUpdateStatus.UNKNOWN, 2, "TIMEOUT", "timeout");
        assertTask(102L, false, ReputationUpdateStatus.PENDING, 0, null, null);
    }

    private MarketReputationUpdateRow row(
            long marketId,
            long predictionId,
            long memberId,
            boolean isCorrect,
            ReputationUpdateStatus status,
            LocalDateTime now
    ) {
        return new MarketReputationUpdateRow(
                null,
                marketId,
                predictionId,
                memberId,
                isCorrect,
                status,
                0,
                null,
                null,
                now,
                now
        );
    }

    private void insertRaw(
            long id,
            long marketId,
            long predictionId,
            long memberId,
            String status,
            LocalDateTime updatedAt
    ) {
        jdbcTemplate.update("""
                INSERT INTO market_reputation_update (
                    id, market_id, prediction_id, member_id, is_correct,
                    status, attempt_no, created_at, updated_at
                ) VALUES (?, ?, ?, ?, TRUE, ?, 0, ?, ?)
                """,
                id,
                marketId,
                predictionId,
                memberId,
                status,
                updatedAt.minusMinutes(1),
                updatedAt
        );
    }

    private void insertMarket(long marketId, long resultOptionId, LocalDateTime now) {
        jdbcTemplate.update("""
                INSERT INTO market (
                    id, title, category, answer_type, judge_data_source, judge_criteria, judge_date,
                    status, close_at, result_option_id, total_pool, initial_virtual_liquidity,
                    created_by, created_at, updated_at
                ) VALUES (?, 'Reputation Mapper Market', 'PRICE_INDEX', 'MULTIPLE_CHOICE',
                          'TEST', 'TEST', ?, 'SETTLED', ?, ?, 100.00, 200.00, 1, ?, ?)
                """,
                marketId,
                LocalDate.now(),
                now.minusDays(1),
                resultOptionId,
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
                ) VALUES (?, ?, ?, ?, ?, 100.00, 0.00, 0.00000000, 0.50000000, 0, FALSE, ?, ?)
                """,
                optionId,
                marketId,
                "O" + optionId,
                "Option " + optionId,
                optionId,
                now,
                now
        );
    }

    private void insertPrediction(
            long predictionId,
            long marketId,
            long optionId,
            long memberId,
            String status,
            LocalDateTime now
    ) {
        jdbcTemplate.update("""
                INSERT INTO market_prediction (
                    id, market_id, option_id, member_id, point_amount, price_snapshot,
                    contract_quantity, status, point_spend_idempotency_key, attempt_no,
                    created_at, updated_at
                ) VALUES (?, ?, ?, ?, 100.00, 0.50000000, 200.00000000, ?, ?, 1, ?, ?)
                """,
                predictionId,
                marketId,
                optionId,
                memberId,
                status,
                "REPUTATION_MAPPER_KEY_" + predictionId,
                now,
                now
        );
    }

    private void assertTask(
            long predictionId,
            boolean isCorrect,
            ReputationUpdateStatus status,
            int attemptNo,
            String errorCode,
            String errorMessage
    ) {
        MarketReputationUpdateRow saved = marketMapper.selectReputationUpdateByPredictionId(predictionId);
        assertThat(saved).isNotNull();
        assertThat(saved.getIsCorrect()).isEqualTo(isCorrect);
        assertThat(saved.getStatus()).isEqualTo(status);
        assertThat(saved.getAttemptNo()).isEqualTo(attemptNo);
        assertThat(saved.getLastErrorCode()).isEqualTo(errorCode);
        assertThat(saved.getLastErrorMessage()).isEqualTo(errorMessage);
    }
}
