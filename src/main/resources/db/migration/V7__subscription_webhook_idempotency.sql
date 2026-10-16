CREATE TABLE subscription_webhook_events (
    id BIGSERIAL PRIMARY KEY,
    shop_id BIGINT NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    subscription_id BIGINT NOT NULL REFERENCES saas_subscriptions(id) ON DELETE CASCADE,
    event_id VARCHAR(128) NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    payload_hash VARCHAR(64) NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_subscription_webhook_event_id UNIQUE (event_id)
);

CREATE INDEX idx_subscription_webhook_shop_processed
    ON subscription_webhook_events(shop_id, processed_at DESC);
