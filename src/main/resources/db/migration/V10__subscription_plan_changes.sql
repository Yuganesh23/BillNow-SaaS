-- Cancellation now takes effect at the end of the paid period, and plan
-- changes are tracked until Razorpay confirms them by webhook.
ALTER TABLE saas_subscriptions ADD COLUMN IF NOT EXISTS cancel_at_period_end BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE saas_subscriptions ADD COLUMN IF NOT EXISTS pending_plan VARCHAR(20);
