-- Subscription payments collected by Razorpay and the GST tax invoices BillNow
-- issues to shops for them. Invoice numbers use one platform-wide series per
-- financial year.
CREATE TABLE IF NOT EXISTS saas_invoice_sequences (
    id BIGSERIAL PRIMARY KEY,
    financial_year VARCHAR(7) NOT NULL,
    last_number INTEGER NOT NULL DEFAULT 0,
    version BIGINT,
    CONSTRAINT uk_saas_invoice_sequence_fy UNIQUE (financial_year)
);

CREATE TABLE IF NOT EXISTS saas_billing_charges (
    id BIGSERIAL PRIMARY KEY,
    shop_id BIGINT NOT NULL REFERENCES shops(id),
    subscription_id BIGINT NOT NULL REFERENCES saas_subscriptions(id),
    razorpay_payment_id VARCHAR(64) NOT NULL,
    razorpay_invoice_id VARCHAR(64),
    plan VARCHAR(20) NOT NULL,
    paid_at TIMESTAMP NOT NULL,
    period_start TIMESTAMP,
    period_end TIMESTAMP,
    currency VARCHAR(3) NOT NULL,
    total_amount NUMERIC(12, 2) NOT NULL,
    invoice_number VARCHAR(16),
    financial_year VARCHAR(7),
    invoice_issued_at TIMESTAMP,
    gst_rate NUMERIC(5, 2),
    taxable_amount NUMERIC(12, 2),
    cgst_amount NUMERIC(12, 2),
    sgst_amount NUMERIC(12, 2),
    igst_amount NUMERIC(12, 2),
    sac_code VARCHAR(8),
    supplier_legal_name VARCHAR(150),
    supplier_gstin VARCHAR(15),
    supplier_address VARCHAR(500),
    supplier_state_code VARCHAR(2),
    recipient_name VARCHAR(150),
    recipient_gstin VARCHAR(15),
    recipient_address VARCHAR(500),
    place_of_supply_code VARCHAR(2),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_saas_charge_payment UNIQUE (razorpay_payment_id),
    CONSTRAINT uk_saas_charge_invoice_number UNIQUE (invoice_number)
);

CREATE INDEX IF NOT EXISTS idx_saas_billing_charges_shop_paid
    ON saas_billing_charges(shop_id, paid_at DESC);
