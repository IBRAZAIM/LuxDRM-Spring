ALTER TABLE IF EXISTS product_sizes
    ADD COLUMN IF NOT EXISTS stock INT NOT NULL DEFAULT 15;

ALTER TABLE IF EXISTS orders
    ADD COLUMN IF NOT EXISTS tracking_number VARCHAR(120),
    ADD COLUMN IF NOT EXISTS promo_code VARCHAR(64);

ALTER TABLE IF EXISTS users
    ADD COLUMN IF NOT EXISTS phone VARCHAR(40),
    ADD COLUMN IF NOT EXISTS country VARCHAR(100),
    ADD COLUMN IF NOT EXISTS city VARCHAR(100),
    ADD COLUMN IF NOT EXISTS address VARCHAR(255),
    ADD COLUMN IF NOT EXISTS postal_code VARCHAR(32),
    ADD COLUMN IF NOT EXISTS reset_token VARCHAR(128),
    ADD COLUMN IF NOT EXISTS reset_token_expiry TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_users_reset_token ON users (reset_token);

CREATE TABLE IF NOT EXISTS promo_codes (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    discount_percent INT NOT NULL,
    description VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    max_uses INT,
    used_count INT NOT NULL DEFAULT 0,
    min_order_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

INSERT INTO promo_codes (code, discount_percent, description, active, max_uses, used_count, min_order_amount)
VALUES
    ('LUX10', 10, 'Скидка 10% по промокоду LUX10', TRUE, NULL, 0, 0),
    ('WELCOME15', 15, 'Приветственная скидка 15%', TRUE, NULL, 0, 0),
    ('VIP20', 20, 'VIP-скидка 20% на весь заказ', TRUE, NULL, 0, 0)
ON CONFLICT (code) DO NOTHING;
