package com.todongsan.marketservice.market.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.todongsan.marketservice.market.type.ReputationUpdateStatus;
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
        jdbcTemplate.update("DELETE FROM market_reputation_update");
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
}
