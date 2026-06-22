package com.todongsan.marketservice.market.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class MarketCommentControllerTest {

    private static final long MARKET_ID = 1L;
    private static final long MEMBER_ID = 2L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM market_comment");
        jdbcTemplate.update("DELETE FROM market_price_history");
        jdbcTemplate.update("DELETE FROM market_reputation_update");
        jdbcTemplate.update("DELETE FROM market_refund_detail");
        jdbcTemplate.update("DELETE FROM market_void");
        jdbcTemplate.update("DELETE FROM market_settlement_detail");
        jdbcTemplate.update("DELETE FROM market_settlement");
        jdbcTemplate.update("DELETE FROM market_prediction");
        jdbcTemplate.update("DELETE FROM market_option");
        jdbcTemplate.update("DELETE FROM market");
    }

    @AfterEach
    void cleanUpComments() {
        jdbcTemplate.update("DELETE FROM market_comment");
    }

    @Test
    void createCommentReturnsCreatedAndPersistsActiveComment() throws Exception {
        insertMarket(MARKET_ID, "ACTIVE", false);

        mockMvc.perform(post("/api/v1/markets/{marketId}/comments", MARKET_ID)
                        .header("X-Member-Id", MEMBER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"이 Market 결과가 궁금합니다.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.commentId").isNumber())
                .andExpect(jsonPath("$.data.marketId").value(MARKET_ID))
                .andExpect(jsonPath("$.data.memberId").value(MEMBER_ID))
                .andExpect(jsonPath("$.data.content").value("이 Market 결과가 궁금합니다."));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM market_comment", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT deleted_at FROM market_comment", LocalDateTime.class)).isNull();
    }

    @Test
    void getCommentsSortsByCreatedAtThenIdAndExcludesDeletedRows() throws Exception {
        insertMarket(MARKET_ID, "ACTIVE", false);
        insertComment(11L, MARKET_ID, 3L, "두 번째", "2026-06-22 10:00:01", null);
        insertComment(10L, MARKET_ID, 2L, "첫 번째", "2026-06-22 10:00:00", null);
        insertComment(12L, MARKET_ID, 4L, "삭제됨", "2026-06-22 10:00:02", "2026-06-22 10:01:00");
        insertComment(9L, MARKET_ID, 5L, "동일 시각 먼저", "2026-06-22 10:00:01", null);

        mockMvc.perform(get("/api/v1/markets/{marketId}/comments", MARKET_ID)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(3))
                .andExpect(jsonPath("$.data.content[0].commentId").value(10))
                .andExpect(jsonPath("$.data.content[1].commentId").value(9))
                .andExpect(jsonPath("$.data.content[2].commentId").value(11))
                .andExpect(jsonPath("$.data.number").value(0))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.first").value(true))
                .andExpect(jsonPath("$.data.last").value(true));
    }

    @Test
    void getCommentsReturnsEmptyPage() throws Exception {
        insertMarket(MARKET_ID, "VOIDED", false);

        mockMvc.perform(get("/api/v1/markets/{marketId}/comments", MARKET_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isEmpty())
                .andExpect(jsonPath("$.data.number").value(0))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.totalElements").value(0))
                .andExpect(jsonPath("$.data.totalPages").value(0))
                .andExpect(jsonPath("$.data.first").value(true))
                .andExpect(jsonPath("$.data.last").value(true));
    }

    @Test
    void deleteCommentSoftDeletesAndSecondDeleteReturnsNotFound() throws Exception {
        insertMarket(MARKET_ID, "ACTIVE", false);
        insertComment(101L, MARKET_ID, MEMBER_ID, "삭제할 댓글", "2026-06-22 10:00:00", null);

        mockMvc.perform(delete("/api/v1/markets/{marketId}/comments/{commentId}", MARKET_ID, 101L)
                        .header("X-Member-Id", MEMBER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.commentId").value(101))
                .andExpect(jsonPath("$.data.deleted").value(true));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT deleted_at FROM market_comment WHERE id = 101",
                LocalDateTime.class
        )).isNotNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT updated_at FROM market_comment WHERE id = 101",
                LocalDateTime.class
        )).isAfter(LocalDateTime.of(2026, 6, 22, 10, 0));

        mockMvc.perform(get("/api/v1/markets/{marketId}/comments", MARKET_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isEmpty());

        mockMvc.perform(delete("/api/v1/markets/{marketId}/comments/{commentId}", MARKET_ID, 101L)
                        .header("X-Member-Id", MEMBER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("MARKET_COMMENT_NOT_FOUND"));
    }

    @Test
    void createAndDeleteWithoutMemberHeaderReturnUnauthorized() throws Exception {
        insertMarket(MARKET_ID, "ACTIVE", false);
        insertComment(101L, MARKET_ID, MEMBER_ID, "댓글", "2026-06-22 10:00:00", null);

        mockMvc.perform(post("/api/v1/markets/{marketId}/comments", MARKET_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"댓글\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        mockMvc.perform(delete("/api/v1/markets/{marketId}/comments/{commentId}", MARKET_ID, 101L))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void blankAndNullContentReturnValidationFailedWithoutInsert() throws Exception {
        insertMarket(MARKET_ID, "ACTIVE", false);

        for (String body : new String[]{"{\"content\":\"   \"}", "{\"content\":null}"}) {
            mockMvc.perform(post("/api/v1/markets/{marketId}/comments", MARKET_ID)
                            .header("X-Member-Id", MEMBER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
        }

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM market_comment", Integer.class)).isZero();
    }

    @Test
    void contentOver500CharactersReturnsCommentTooLong() throws Exception {
        insertMarket(MARKET_ID, "ACTIVE", false);
        String content = "가".repeat(501);

        mockMvc.perform(post("/api/v1/markets/{marketId}/comments", MARKET_ID)
                        .header("X-Member-Id", MEMBER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"" + content + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MARKET_COMMENT_TOO_LONG"));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM market_comment", Integer.class)).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"PENDING", "VOIDED"})
    void blockedMarketStatusesRejectComment(String statusValue) throws Exception {
        insertMarket(MARKET_ID, statusValue, false);

        createCommentRequest(MARKET_ID, MEMBER_ID)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("MARKET_COMMENT_NOT_ALLOWED"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ACTIVE", "CLOSED", "DATA_PENDING", "SETTLEMENT_IN_PROGRESS", "SETTLED"})
    void allowedMarketStatusesAcceptComment(String statusValue) throws Exception {
        insertMarket(MARKET_ID, statusValue, false);

        createCommentRequest(MARKET_ID, MEMBER_ID)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.content").value("댓글"));
    }

    @Test
    void missingOrDeletedMarketReturnsMarketNotFoundForCreateAndList() throws Exception {
        for (long marketId : new long[]{99L, 100L}) {
            if (marketId == 100L) {
                insertMarket(marketId, "ACTIVE", true);
            }
            createCommentRequest(marketId, MEMBER_ID)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value("MARKET_NOT_FOUND"));
            mockMvc.perform(get("/api/v1/markets/{marketId}/comments", marketId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value("MARKET_NOT_FOUND"));
        }
    }

    @Test
    void deletingAnotherMembersCommentReturnsForbiddenWithoutMutation() throws Exception {
        insertMarket(MARKET_ID, "ACTIVE", false);
        insertComment(101L, MARKET_ID, 3L, "타인 댓글", "2026-06-22 10:00:00", null);

        mockMvc.perform(delete("/api/v1/markets/{marketId}/comments/{commentId}", MARKET_ID, 101L)
                        .header("X-Member-Id", MEMBER_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("MARKET_COMMENT_FORBIDDEN"));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT deleted_at FROM market_comment WHERE id = 101",
                LocalDateTime.class
        )).isNull();
    }

    @Test
    void missingDeletedAndMarketMismatchedCommentsReturnNotFound() throws Exception {
        insertMarket(MARKET_ID, "ACTIVE", false);
        insertMarket(2L, "ACTIVE", false);
        insertComment(101L, MARKET_ID, MEMBER_ID, "삭제됨", "2026-06-22 10:00:00", "2026-06-22 10:01:00");
        insertComment(102L, MARKET_ID, MEMBER_ID, "다른 Market 경로", "2026-06-22 10:00:00", null);

        for (long[] target : new long[][]{{MARKET_ID, 999L}, {MARKET_ID, 101L}, {2L, 102L}}) {
            mockMvc.perform(delete("/api/v1/markets/{marketId}/comments/{commentId}", target[0], target[1])
                            .header("X-Member-Id", MEMBER_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value("MARKET_COMMENT_NOT_FOUND"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "abc"})
    void invalidPageReturnsValidationFailed(String page) throws Exception {
        insertMarket(MARKET_ID, "ACTIVE", false);

        mockMvc.perform(get("/api/v1/markets/{marketId}/comments", MARKET_ID).param("page", page))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "101", "abc"})
    void invalidSizeReturnsValidationFailed(String size) throws Exception {
        insertMarket(MARKET_ID, "ACTIVE", false);

        mockMvc.perform(get("/api/v1/markets/{marketId}/comments", MARKET_ID).param("size", size))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    void commentCreateAndDeleteDoNotChangeMarketFinancialOrWorkflowData() throws Exception {
        insertMarket(MARKET_ID, "ACTIVE", false);
        String poolBefore = jdbcTemplate.queryForObject(
                "SELECT total_pool FROM market WHERE id = ?",
                String.class,
                MARKET_ID
        );

        createCommentRequest(MARKET_ID, MEMBER_ID).andExpect(status().isCreated());
        Long commentId = jdbcTemplate.queryForObject("SELECT id FROM market_comment", Long.class);
        mockMvc.perform(delete("/api/v1/markets/{marketId}/comments/{commentId}", MARKET_ID, commentId)
                        .header("X-Member-Id", MEMBER_ID))
                .andExpect(status().isOk());

        assertThat(jdbcTemplate.queryForObject("SELECT total_pool FROM market WHERE id = ?", String.class, MARKET_ID))
                .isEqualTo(poolBefore);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM market_prediction", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM market_settlement", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM market_refund_detail", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM market_reputation_update", Integer.class)).isZero();
    }

    private org.springframework.test.web.servlet.ResultActions createCommentRequest(long marketId, long memberId)
            throws Exception {
        return mockMvc.perform(post("/api/v1/markets/{marketId}/comments", marketId)
                .header("X-Member-Id", memberId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"댓글\"}"));
    }

    private void insertMarket(long marketId, String status, boolean deleted) {
        jdbcTemplate.update("""
                INSERT INTO market (
                    id, title, description, category, answer_type, judge_data_source,
                    judge_criteria, judge_date, status, close_at, settle_due_at,
                    total_pool, created_by, deleted_at, created_at, updated_at
                ) VALUES (?, ?, '설명', 'PRICE_INDEX', 'NUMERIC_RANGE', '한국부동산원',
                    '판정 기준', '2026-06-30', ?, '2026-06-29 18:00:00', '2026-06-30 18:00:00',
                    123.00, 1, ?, '2026-06-22 09:00:00', '2026-06-22 09:00:00')
                """,
                marketId,
                "Market " + marketId,
                status,
                deleted ? LocalDateTime.of(2026, 6, 22, 9, 30) : null
        );
    }

    private void insertComment(
            long commentId,
            long marketId,
            long memberId,
            String content,
            String createdAt,
            String deletedAt
    ) {
        jdbcTemplate.update("""
                INSERT INTO market_comment (
                    id, market_id, member_id, content, deleted_at, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                commentId,
                marketId,
                memberId,
                content,
                deletedAt,
                createdAt,
                createdAt
        );
    }
}
