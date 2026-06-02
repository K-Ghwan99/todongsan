DROP TABLE IF EXISTS market_price_history;
DROP TABLE IF EXISTS market_prediction;
DROP TABLE IF EXISTS market_option;
DROP TABLE IF EXISTS market;

CREATE TABLE market (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(50) NOT NULL,
    answer_type VARCHAR(30) NOT NULL,
    metric_unit VARCHAR(30),
    judge_data_source VARCHAR(255) NOT NULL,
    judge_criteria TEXT NOT NULL,
    judge_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    close_at DATETIME NOT NULL,
    settle_due_at DATETIME,
    settled_at DATETIME,
    result_option_id BIGINT,
    result_value DECIMAL(12,4),
    result_text VARCHAR(255),
    total_pool DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    fee_rate DECIMAL(5,2) NOT NULL DEFAULT 5.00,
    fee_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    settlement_pool DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    initial_virtual_liquidity DECIMAL(10,2) NOT NULL DEFAULT 100.00,
    price_model VARCHAR(30) NOT NULL DEFAULT 'POOL_SHARE',
    created_by BIGINT NOT NULL,
    deleted_at DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE market_option (
    id BIGINT NOT NULL AUTO_INCREMENT,
    market_id BIGINT NOT NULL,
    option_code VARCHAR(20) NOT NULL,
    option_text VARCHAR(100) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    range_min DECIMAL(12,4),
    range_max DECIMAL(12,4),
    min_inclusive BOOLEAN NOT NULL DEFAULT TRUE,
    max_inclusive BOOLEAN NOT NULL DEFAULT FALSE,
    virtual_pool_amount DECIMAL(10,2) NOT NULL DEFAULT 100.00,
    real_pool_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    total_contract_quantity DECIMAL(24,8) NOT NULL DEFAULT 0.00000000,
    current_price DECIMAL(18,8) NOT NULL DEFAULT 0.00000000,
    prediction_count INT NOT NULL DEFAULT 0,
    is_result BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_market_option_code (market_id, option_code),
    CONSTRAINT fk_market_option_market
        FOREIGN KEY (market_id)
        REFERENCES market(id)
);

CREATE TABLE market_prediction (
    id BIGINT NOT NULL AUTO_INCREMENT,
    market_id BIGINT NOT NULL,
    option_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    point_amount DECIMAL(10,2) NOT NULL,
    price_snapshot DECIMAL(18,8),
    contract_quantity DECIMAL(24,8),
    expected_payout_per_contract_snapshot DECIMAL(18,8),
    expected_multiplier_snapshot DECIMAL(18,8),
    status VARCHAR(30) NOT NULL DEFAULT 'POINT_PENDING',
    point_spend_idempotency_key VARCHAR(150) NOT NULL UNIQUE,
    attempt_no INT NOT NULL DEFAULT 1,
    settled_amount DECIMAL(10,2),
    refund_amount DECIMAL(10,2),
    fail_reason VARCHAR(255),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_market_prediction_member (market_id, member_id),
    CONSTRAINT fk_market_prediction_market
        FOREIGN KEY (market_id)
        REFERENCES market(id),
    CONSTRAINT fk_market_prediction_option
        FOREIGN KEY (option_id)
        REFERENCES market_option(id),
    CONSTRAINT chk_market_prediction_point_amount
        CHECK (point_amount >= 10 AND point_amount <= 500)
);

CREATE TABLE market_price_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    market_id BIGINT NOT NULL,
    option_id BIGINT NOT NULL,
    prediction_id BIGINT,
    price_before DECIMAL(18,8) NOT NULL,
    price_after DECIMAL(18,8) NOT NULL,
    real_pool_before DECIMAL(10,2) NOT NULL,
    real_pool_after DECIMAL(10,2) NOT NULL,
    contract_quantity_before DECIMAL(24,8) NOT NULL,
    contract_quantity_after DECIMAL(24,8) NOT NULL,
    event_type VARCHAR(30) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_price_history_market
        FOREIGN KEY (market_id)
        REFERENCES market(id),
    CONSTRAINT fk_price_history_option
        FOREIGN KEY (option_id)
        REFERENCES market_option(id),
    CONSTRAINT fk_price_history_prediction
        FOREIGN KEY (prediction_id)
        REFERENCES market_prediction(id)
);
