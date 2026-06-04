USE market;

CREATE TABLE IF NOT EXISTS market_refund_detail (
    id BIGINT NOT NULL AUTO_INCREMENT,
    market_void_id BIGINT NOT NULL,
    prediction_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    refund_amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    idempotency_key VARCHAR(150) NOT NULL,
    fail_reason VARCHAR(255),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_refund_detail_prediction (prediction_id),
    UNIQUE KEY uq_refund_detail_idempotency_key (idempotency_key),
    KEY idx_refund_detail_void_status (market_void_id, status),
    KEY idx_refund_detail_member (member_id),
    CONSTRAINT fk_refund_detail_market_void
        FOREIGN KEY (market_void_id)
        REFERENCES market_void(id),
    CONSTRAINT fk_refund_detail_prediction
        FOREIGN KEY (prediction_id)
        REFERENCES market_prediction(id)
);
