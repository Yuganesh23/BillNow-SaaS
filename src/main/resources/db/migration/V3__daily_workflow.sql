ALTER TABLE customers ADD COLUMN IF NOT EXISTS credit_balance NUMERIC(12,2) NOT NULL DEFAULT 0.00;

ALTER TABLE payments DROP CONSTRAINT IF EXISTS payments_invoice_id_key;
DO $$
DECLARE constraint_name TEXT;
BEGIN
  SELECT con.conname INTO constraint_name
  FROM pg_constraint con
  JOIN pg_class rel ON rel.oid = con.conrelid
  JOIN pg_attribute att ON att.attrelid = rel.oid AND att.attnum = ANY(con.conkey)
  WHERE rel.relname = 'payments' AND con.contype = 'u' AND att.attname = 'invoice_id'
  LIMIT 1;
  IF constraint_name IS NOT NULL THEN
    EXECUTE format('ALTER TABLE payments DROP CONSTRAINT %I', constraint_name);
  END IF;
END $$;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS reference VARCHAR(100);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS notes VARCHAR(500);
CREATE INDEX IF NOT EXISTS idx_payments_invoice_paid_at ON payments(invoice_id, paid_at);

CREATE TABLE IF NOT EXISTS quotations (
    id BIGSERIAL PRIMARY KEY,
    shop_id BIGINT NOT NULL REFERENCES shops(id),
    customer_id BIGINT NOT NULL REFERENCES customers(id),
    created_by BIGINT NOT NULL REFERENCES users(id),
    quote_number VARCHAR(30) NOT NULL,
    document_kind VARCHAR(25) NOT NULL DEFAULT 'QUOTATION',
    status VARCHAR(20) NOT NULL,
    valid_until DATE,
    taxable_amount NUMERIC(12,2) NOT NULL,
    discount_amount NUMERIC(12,2) NOT NULL,
    cgst_total NUMERIC(12,2) NOT NULL,
    sgst_total NUMERIC(12,2) NOT NULL,
    igst_total NUMERIC(12,2) NOT NULL,
    cess_total NUMERIC(12,2) NOT NULL,
    round_off NUMERIC(12,2) NOT NULL,
    total_amount NUMERIC(12,2) NOT NULL,
    converted_invoice_id BIGINT REFERENCES invoices(id),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_quotation_number UNIQUE(shop_id, quote_number)
);

CREATE TABLE IF NOT EXISTS quotation_items (
    id BIGSERIAL PRIMARY KEY,
    quotation_id BIGINT NOT NULL REFERENCES quotations(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id),
    quantity NUMERIC(12,3) NOT NULL,
    unit_price NUMERIC(12,2) NOT NULL,
    line_discount NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    taxable_amount NUMERIC(12,2) NOT NULL,
    gst_rate NUMERIC(5,2) NOT NULL,
    cgst NUMERIC(12,2) NOT NULL,
    sgst NUMERIC(12,2) NOT NULL,
    igst NUMERIC(12,2) NOT NULL,
    cess NUMERIC(12,2) NOT NULL,
    total_amount NUMERIC(12,2) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_quotations_shop_created ON quotations(shop_id, created_at DESC);

CREATE TABLE IF NOT EXISTS recurring_invoices (
    id BIGSERIAL PRIMARY KEY,
    shop_id BIGINT NOT NULL REFERENCES shops(id),
    customer_id BIGINT NOT NULL REFERENCES customers(id),
    created_by BIGINT NOT NULL REFERENCES users(id),
    frequency VARCHAR(20) NOT NULL,
    next_run_date DATE NOT NULL,
    issue_automatically BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    discount_amount NUMERIC(12,2) NOT NULL DEFAULT 0.00
);
CREATE TABLE IF NOT EXISTS recurring_invoice_items (
    id BIGSERIAL PRIMARY KEY,
    recurring_invoice_id BIGINT NOT NULL REFERENCES recurring_invoices(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id),
    quantity NUMERIC(12,3) NOT NULL,
    line_discount NUMERIC(12,2) NOT NULL DEFAULT 0.00
);
CREATE TABLE IF NOT EXISTS recurring_invoice_runs (
    id BIGSERIAL PRIMARY KEY,
    recurring_invoice_id BIGINT NOT NULL REFERENCES recurring_invoices(id),
    scheduled_date DATE NOT NULL,
    invoice_id BIGINT NOT NULL REFERENCES invoices(id),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_recurring_run UNIQUE(recurring_invoice_id, scheduled_date)
);

CREATE TABLE IF NOT EXISTS reminder_logs (
    id BIGSERIAL PRIMARY KEY,
    shop_id BIGINT NOT NULL REFERENCES shops(id),
    customer_id BIGINT NOT NULL REFERENCES customers(id),
    invoice_id BIGINT NOT NULL REFERENCES invoices(id),
    channel VARCHAR(20) NOT NULL,
    reminder_key VARCHAR(100) NOT NULL,
    sent_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_reminder_delivery UNIQUE(shop_id, reminder_key, channel)
);
