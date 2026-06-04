USE market;

-- Existing Docker volumes do not rerun docker-entrypoint-initdb.d scripts.
-- Apply this additive migration once when upgrading a Market DB created before FAILED retry support.
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
