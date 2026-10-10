-- Additive migration for databases originally created by Hibernate.
-- Existing installations are baselined at version 0, then receive this migration.

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expires ON refresh_tokens(expires_at);

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    code_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    attempts INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_password_reset_user_created
    ON password_reset_tokens(user_id, created_at DESC);

ALTER TABLE shops ADD COLUMN IF NOT EXISTS trial_ends_at TIMESTAMP;
ALTER TABLE shops ADD COLUMN IF NOT EXISTS logo_object_key VARCHAR(500);
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(100);
CREATE UNIQUE INDEX IF NOT EXISTS uk_invoice_shop_idempotency
    ON invoices(shop_id, idempotency_key) WHERE idempotency_key IS NOT NULL;
UPDATE shops
SET trial_ends_at = CURRENT_TIMESTAMP + INTERVAL '7 days'
WHERE subscription_tier = 'TRIAL' AND trial_ends_at IS NULL;

UPDATE shops s
SET last_invoice_number = GREATEST(
    COALESCE(s.last_invoice_number, 0),
    COALESCE((
        SELECT MAX(NULLIF(regexp_replace(i.invoice_number, '[^0-9]', '', 'g'), '')::INTEGER)
        FROM invoices i
        WHERE i.shop_id = s.id
    ), 0)
);

CREATE INDEX IF NOT EXISTS idx_users_shop_id ON users(shop_id);
CREATE INDEX IF NOT EXISTS idx_invoices_shop_id ON invoices(shop_id);
CREATE INDEX IF NOT EXISTS idx_customers_shop_id ON customers(shop_id);
CREATE INDEX IF NOT EXISTS idx_products_shop_id ON products(shop_id);
CREATE INDEX IF NOT EXISTS idx_payments_invoice_id ON payments(invoice_id);

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    shop_id BIGINT,
    actor_user_id BIGINT,
    method VARCHAR(10) NOT NULL,
    path VARCHAR(500) NOT NULL,
    status INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_audit_shop_created ON audit_logs(shop_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_actor ON audit_logs(actor_user_id);
