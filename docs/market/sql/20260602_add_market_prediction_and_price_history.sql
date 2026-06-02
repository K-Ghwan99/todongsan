USE market;

-- Existing Docker volumes do not rerun docker-entrypoint-initdb.d scripts.
-- Apply this additive migration once when upgrading an existing Market DB.
CREATE TABLE IF NOT EXISTS market_prediction (
    id                                      BIGINT          NOT NULL AUTO_INCREMENT,
    market_id                               BIGINT          NOT NULL,
    option_id                               BIGINT          NOT NULL,
    member_id                               BIGINT          NOT NULL,
    point_amount                            DECIMAL(10,2)   NOT NULL,
    price_snapshot                          DECIMAL(18,8),
    contract_quantity                       DECIMAL(24,8),
    expected_payout_per_contract_snapshot   DECIMAL(18,8),
    expected_multiplier_snapshot            DECIMAL(18,8),
    status                                  VARCHAR(30)     NOT NULL DEFAULT 'POINT_PENDING',
    point_spend_idempotency_key             VARCHAR(150)    NOT NULL UNIQUE,
    attempt_no                              INT             NOT NULL DEFAULT 1,
    settled_amount                          DECIMAL(10,2),
    refund_amount                           DECIMAL(10,2),
    fail_reason                             VARCHAR(255),
    created_at                              DATETIME        NOT NULL,
    updated_at                              DATETIME        NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_market_prediction_member (market_id, member_id),
    INDEX idx_market_prediction_market_status (market_id, status),
    INDEX idx_market_prediction_market_option (market_id, option_id),
    INDEX idx_market_prediction_market_created (market_id, created_at),
    INDEX idx_market_prediction_member_id (member_id),
    INDEX idx_market_prediction_option_status (option_id, status),
    INDEX idx_market_prediction_point_spend_key (point_spend_idempotency_key),
    CONSTRAINT fk_market_prediction_market
        FOREIGN KEY (market_id)
        REFERENCES market(id),
    CONSTRAINT fk_market_prediction_option
        FOREIGN KEY (option_id)
        REFERENCES market_option(id),
    CONSTRAINT chk_market_prediction_point_amount
        CHECK (point_amount >= 10 AND point_amount <= 500)
);

SET @add_market_prediction_attempt_no = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE market_prediction ADD COLUMN attempt_no INT NOT NULL DEFAULT 1 AFTER point_spend_idempotency_key',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'market_prediction'
      AND COLUMN_NAME = 'attempt_no'
);
PREPARE add_market_prediction_attempt_no FROM @add_market_prediction_attempt_no;
EXECUTE add_market_prediction_attempt_no;
DEALLOCATE PREPARE add_market_prediction_attempt_no;

CREATE TABLE IF NOT EXISTS market_price_history (
    id                              BIGINT          NOT NULL AUTO_INCREMENT,
    market_id                       BIGINT          NOT NULL,
    option_id                       BIGINT          NOT NULL,
    prediction_id                   BIGINT,
    price_before                    DECIMAL(18,8)   NOT NULL,
    price_after                     DECIMAL(18,8)   NOT NULL,
    real_pool_before                DECIMAL(10,2)   NOT NULL,
    real_pool_after                 DECIMAL(10,2)   NOT NULL,
    contract_quantity_before        DECIMAL(24,8)   NOT NULL,
    contract_quantity_after         DECIMAL(24,8)   NOT NULL,
    event_type                      VARCHAR(30)     NOT NULL DEFAULT 'PREDICTION_CONFIRMED',
    created_at                      DATETIME        NOT NULL,
    updated_at                      DATETIME        NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_market_price_history_market_created (market_id, created_at, id),
    INDEX idx_market_price_history_option_created (option_id, created_at, id),
    CONSTRAINT fk_price_history_market
        FOREIGN KEY (market_id)
        REFERENCES market(id),
    CONSTRAINT fk_price_history_option
        FOREIGN KEY (option_id)
        REFERENCES market_option(id)
);

SET @add_price_history_prediction_fk = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE market_price_history ADD CONSTRAINT fk_price_history_prediction FOREIGN KEY (prediction_id) REFERENCES market_prediction(id)',
        'SELECT 1'
    )
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'market_price_history'
      AND CONSTRAINT_NAME = 'fk_price_history_prediction'
      AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);

PREPARE add_price_history_prediction_fk FROM @add_price_history_prediction_fk;
EXECUTE add_price_history_prediction_fk;
DEALLOCATE PREPARE add_price_history_prediction_fk;
