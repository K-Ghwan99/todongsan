USE market;

-- Existing Docker volumes do not rerun docker-entrypoint-initdb.d scripts.
-- Apply this additive migration once when enabling Market comments.
CREATE TABLE IF NOT EXISTS market_comment (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    market_id   BIGINT       NOT NULL,
    member_id   BIGINT       NOT NULL,
    content     TEXT         NOT NULL,
    deleted_at  DATETIME     NULL,
    created_at  DATETIME     NOT NULL,
    updated_at  DATETIME     NOT NULL,

    PRIMARY KEY (id),
    INDEX idx_market_comment_list (market_id, deleted_at, created_at, id),

    CONSTRAINT fk_market_comment_market
        FOREIGN KEY (market_id)
        REFERENCES market(id)
);
