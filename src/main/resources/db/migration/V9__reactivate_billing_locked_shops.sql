-- Subscription state used to switch shops.active off, which locked owners out of
-- their customers and invoices. Billing access is now enforced from
-- subscription_tier/subscription_ends_at only, and nothing else sets a shop
-- inactive, so every shop disabled that way is restored.
UPDATE shops SET active = TRUE WHERE active = FALSE;
