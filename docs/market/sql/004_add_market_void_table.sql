USE market;

-- Existing Docker volumes do not rerun docker-entrypoint-initdb.d scripts.
-- Apply this additive migration once when enabling Market void handling.
CREATE TABLE IF NOT EXISTS market_void (
    id                              BIGINT          NOT NULL AUTO_INCREMENT,
    market_id                       BIGINT          NOT NULL,
    reason_type                     VARCHAR(50)     NOT NULL,
    reason_detail                   TEXT,
    refund_status                   VARCHAR(30)     NOT NULL DEFAULT 'PENDING',
    voided_by                       BIGINT,
    voided_at                       DATETIME        NOT NULL,
    created_at                      DATETIME        NOT NULL,
    updated_at                      DATETIME        NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_market_void_market (market_id),
    CONSTRAINT fk_market_void_market
        FOREIGN KEY (market_id)
        REFERENCES market(id)
);
