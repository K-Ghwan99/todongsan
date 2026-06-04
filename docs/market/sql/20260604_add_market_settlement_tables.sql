USE market;

-- Existing Docker volumes do not rerun docker-entrypoint-initdb.d scripts.
-- Apply this additive migration once when enabling Market settlement.
CREATE TABLE IF NOT EXISTS market_settlement (
    id                              BIGINT          NOT NULL AUTO_INCREMENT,
    market_id                       BIGINT          NOT NULL,
    result_option_id                BIGINT          NOT NULL,
    total_pool                      DECIMAL(10,2)   NOT NULL,
    fee_rate                        DECIMAL(5,2)    NOT NULL,
    fee_amount                      DECIMAL(10,2)   NOT NULL,
    settlement_pool                 DECIMAL(10,2)   NOT NULL,
    winning_contract_quantity       DECIMAL(24,8)   NOT NULL,
    payout_per_contract             DECIMAL(18,8)   NOT NULL,
    burned_point_amount             DECIMAL(10,2)   NOT NULL DEFAULT 0.00,
    status                          VARCHAR(30)     NOT NULL DEFAULT 'PENDING',
    settled_by                      BIGINT,
    settled_at                      DATETIME,
    created_at                      DATETIME        NOT NULL,
    updated_at                      DATETIME        NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_market_settlement_market (market_id),
    CONSTRAINT fk_market_settlement_market
        FOREIGN KEY (market_id)
        REFERENCES market(id),
    CONSTRAINT fk_market_settlement_result_option
        FOREIGN KEY (result_option_id)
        REFERENCES market_option(id)
);

CREATE TABLE IF NOT EXISTS market_settlement_detail (
    id                              BIGINT          NOT NULL AUTO_INCREMENT,
    settlement_id                   BIGINT          NOT NULL,
    prediction_id                   BIGINT          NOT NULL,
    member_id                       BIGINT          NOT NULL,
    original_point_amount           DECIMAL(10,2)   NOT NULL,
    contract_quantity               DECIMAL(24,8)   NOT NULL,
    payout_per_contract             DECIMAL(18,8)   NOT NULL,
    settled_amount                  DECIMAL(10,2)   NOT NULL,
    profit_amount                   DECIMAL(10,2)   NOT NULL,
    status                          VARCHAR(30)     NOT NULL DEFAULT 'PENDING',
    idempotency_key                 VARCHAR(150)    NOT NULL UNIQUE,
    fail_reason                     VARCHAR(255),
    created_at                      DATETIME        NOT NULL,
    updated_at                      DATETIME        NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_settlement_detail_prediction (prediction_id),
    INDEX idx_settlement_detail_member_id (member_id),
    INDEX idx_settlement_detail_settlement_id (settlement_id),
    INDEX idx_settlement_detail_status (status),
    INDEX idx_settlement_detail_idempotency_key (idempotency_key),
    CONSTRAINT fk_settlement_detail_settlement
        FOREIGN KEY (settlement_id)
        REFERENCES market_settlement(id),
    CONSTRAINT fk_settlement_detail_prediction
        FOREIGN KEY (prediction_id)
        REFERENCES market_prediction(id)
);
