USE market;

-- Existing Docker volumes do not rerun docker-entrypoint-initdb.d scripts.
-- Apply this additive migration once when enabling Market reputation update outbox.
CREATE TABLE IF NOT EXISTS market_reputation_update (
    id                              BIGINT          NOT NULL AUTO_INCREMENT,
    market_id                       BIGINT          NOT NULL,
    prediction_id                   BIGINT          NOT NULL,
    member_id                       BIGINT          NOT NULL,
    is_correct                      BOOLEAN         NOT NULL,
    status                          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    attempt_no                      INT             NOT NULL DEFAULT 0,
    last_error_code                 VARCHAR(100),
    last_error_message              VARCHAR(500),
    created_at                      DATETIME        NOT NULL,
    updated_at                      DATETIME        NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_reputation_update_prediction (prediction_id),
    INDEX idx_reputation_update_status_updated (status, updated_at, id),
    INDEX idx_reputation_update_market (market_id),
    INDEX idx_reputation_update_member_market (member_id, market_id)
);
