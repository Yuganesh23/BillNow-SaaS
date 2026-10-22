-- Additive GST core. Existing rows receive conservative defaults and remain readable.
ALTER TABLE shops ADD COLUMN IF NOT EXISTS state_code VARCHAR(2);
UPDATE shops SET state_code = SUBSTRING(UPPER(gstin), 1, 2)
WHERE state_code IS NULL AND gstin ~ '^[0-9]{2}' AND SUBSTRING(gstin, 1, 2) IN
('01','02','03','04','05','06','07','08','09','10','11','12','13','14','15','16','17','18','19','20','21','22','23','24','26','27','29','30','31','32','33','34','35','36','37','38','97');
UPDATE shops SET state_code = '97' WHERE state_code IS NULL;
ALTER TABLE shops ALTER COLUMN state_code SET NOT NULL;
ALTER TABLE shops ADD COLUMN IF NOT EXISTS registration_type VARCHAR(20) NOT NULL DEFAULT 'UNREGISTERED';
ALTER TABLE shops ADD COLUMN IF NOT EXISTS hsn_code_length INTEGER NOT NULL DEFAULT 4;
ALTER TABLE shops ADD COLUMN IF NOT EXISTS invoice_prefix VARCHAR(8) NOT NULL DEFAULT 'INV';
ALTER TABLE shops ADD COLUMN IF NOT EXISTS credit_note_prefix VARCHAR(8) NOT NULL DEFAULT 'CN';
ALTER TABLE shops ADD COLUMN IF NOT EXISTS debit_note_prefix VARCHAR(8) NOT NULL DEFAULT 'DN';
ALTER TABLE shops ADD COLUMN IF NOT EXISTS bank_details TEXT;
ALTER TABLE shops ADD COLUMN IF NOT EXISTS invoice_terms TEXT;
ALTER TABLE shops ADD COLUMN IF NOT EXISTS upi_id VARCHAR(100);

ALTER TABLE customers ADD COLUMN IF NOT EXISTS state_code VARCHAR(2);
ALTER TABLE customers ADD COLUMN IF NOT EXISTS billing_address VARCHAR(500);
ALTER TABLE customers ADD COLUMN IF NOT EXISTS shipping_address VARCHAR(500);
UPDATE customers SET billing_address = address WHERE billing_address IS NULL;
ALTER TABLE suppliers ADD COLUMN IF NOT EXISTS state_code VARCHAR(2);

ALTER TABLE products ADD COLUMN IF NOT EXISTS uqc VARCHAR(10) NOT NULL DEFAULT 'NOS';
ALTER TABLE products ADD COLUMN IF NOT EXISTS cess_rate NUMERIC(5,2) NOT NULL DEFAULT 0.00;

ALTER TABLE invoice_items ADD COLUMN IF NOT EXISTS line_discount NUMERIC(12,2) NOT NULL DEFAULT 0.00;
ALTER TABLE invoice_items ADD COLUMN IF NOT EXISTS cess NUMERIC(12,2) NOT NULL DEFAULT 0.00;
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS cess_total NUMERIC(12,2) NOT NULL DEFAULT 0.00;
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS round_off NUMERIC(12,2) NOT NULL DEFAULT 0.00;
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS place_of_supply_code VARCHAR(2);
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS document_type VARCHAR(20) NOT NULL DEFAULT 'TAX_INVOICE';
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS reverse_charge BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS cancellation_reason VARCHAR(500);
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS financial_year VARCHAR(7);
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS series_number INTEGER;

CREATE TABLE IF NOT EXISTS document_sequences (
    id BIGSERIAL PRIMARY KEY,
    shop_id BIGINT NOT NULL REFERENCES shops(id),
    financial_year VARCHAR(7) NOT NULL,
    document_type VARCHAR(20) NOT NULL,
    last_number INTEGER NOT NULL DEFAULT 0,
    version BIGINT,
    CONSTRAINT uk_document_sequence UNIQUE (shop_id, financial_year, document_type)
);

-- Backfill historical invoices without changing their displayed numbers.
UPDATE invoices
SET financial_year = CASE
    WHEN EXTRACT(MONTH FROM created_at) >= 4
      THEN EXTRACT(YEAR FROM created_at)::INTEGER || '-' || RIGHT((EXTRACT(YEAR FROM created_at)::INTEGER + 1)::TEXT, 2)
    ELSE (EXTRACT(YEAR FROM created_at)::INTEGER - 1) || '-' || RIGHT(EXTRACT(YEAR FROM created_at)::INTEGER::TEXT, 2)
END
WHERE financial_year IS NULL AND created_at IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_document_sequences_shop_fy ON document_sequences(shop_id, financial_year);
CREATE INDEX IF NOT EXISTS idx_invoices_shop_fy_series ON invoices(shop_id, financial_year, series_number);

CREATE TABLE IF NOT EXISTS document_notes (
    id BIGSERIAL PRIMARY KEY,
    shop_id BIGINT NOT NULL REFERENCES shops(id),
    original_invoice_id BIGINT NOT NULL REFERENCES invoices(id),
    note_number VARCHAR(16) NOT NULL,
    note_type VARCHAR(20) NOT NULL,
    financial_year VARCHAR(7) NOT NULL,
    series_number INTEGER NOT NULL,
    amount NUMERIC(12,2) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    restock BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_document_note_number UNIQUE (shop_id, note_number)
);
CREATE INDEX IF NOT EXISTS idx_document_notes_shop_invoice ON document_notes(shop_id, original_invoice_id);

ALTER TABLE shops ADD CONSTRAINT ck_shops_hsn_code_length CHECK (hsn_code_length IN (4, 6, 8));
ALTER TABLE shops ADD CONSTRAINT ck_shops_registration_type CHECK (registration_type IN ('REGULAR', 'COMPOSITION', 'UNREGISTERED'));
