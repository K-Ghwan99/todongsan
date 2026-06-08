USE market;

SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE market_price_history ADD COLUMN prediction_id BIGINT NULL',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'market_price_history'
      AND COLUMN_NAME = 'prediction_id'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE market_price_history ADD COLUMN event_type VARCHAR(30) NOT NULL DEFAULT ''PREDICTION_CONFIRMED''',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'market_price_history'
      AND COLUMN_NAME = 'event_type'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE market_price_history ADD COLUMN price_before DECIMAL(18,8) NOT NULL DEFAULT 0.00000000',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'market_price_history'
      AND COLUMN_NAME = 'price_before'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE market_price_history ADD COLUMN price_after DECIMAL(18,8) NOT NULL DEFAULT 0.00000000',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'market_price_history'
      AND COLUMN_NAME = 'price_after'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE market_price_history ADD COLUMN real_pool_before DECIMAL(10,2) NOT NULL DEFAULT 0.00',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'market_price_history'
      AND COLUMN_NAME = 'real_pool_before'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE market_price_history ADD COLUMN real_pool_after DECIMAL(10,2) NOT NULL DEFAULT 0.00',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'market_price_history'
      AND COLUMN_NAME = 'real_pool_after'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE market_price_history ADD COLUMN contract_quantity_before DECIMAL(24,8) NOT NULL DEFAULT 0.00000000',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'market_price_history'
      AND COLUMN_NAME = 'contract_quantity_before'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE market_price_history ADD COLUMN contract_quantity_after DECIMAL(24,8) NOT NULL DEFAULT 0.00000000',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'market_price_history'
      AND COLUMN_NAME = 'contract_quantity_after'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'CREATE INDEX idx_price_history_market_option_created ON market_price_history (market_id, option_id, created_at, id)',
        'SELECT 1'
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'market_price_history'
      AND INDEX_NAME = 'idx_price_history_market_option_created'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
        COUNT(*) = 0,
        'CREATE INDEX idx_price_history_prediction ON market_price_history (prediction_id)',
        'SELECT 1'
    )
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'market_price_history'
      AND INDEX_NAME = 'idx_price_history_prediction'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
