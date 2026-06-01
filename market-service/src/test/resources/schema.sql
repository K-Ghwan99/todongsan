DROP TABLE IF EXISTS market_price_history;
DROP TABLE IF EXISTS market_option;
DROP TABLE IF EXISTS market;

CREATE TABLE market (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(30) NOT NULL,
    close_at DATETIME NOT NULL,
    settle_due_at DATETIME,
    total_pool DECIMAL(10,2) NOT NULL DEFAULT 0.00,
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
    CONSTRAINT fk_market_option_market
        FOREIGN KEY (market_id)
        REFERENCES market(id)
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
        REFERENCES market_option(id)
);
