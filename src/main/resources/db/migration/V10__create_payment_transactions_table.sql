CREATE TABLE IF NOT EXISTS payment_transactions (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    external_transaction_id VARCHAR(120) NOT NULL UNIQUE,
    provider VARCHAR(64) NOT NULL,
    payment_status VARCHAR(40) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'KZT',
    payment_url VARCHAR(512),
    masked_pan VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    paid_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_payment_tx_order ON payment_transactions(order_id);
CREATE INDEX IF NOT EXISTS idx_payment_tx_ext_id ON payment_transactions(external_transaction_id);

