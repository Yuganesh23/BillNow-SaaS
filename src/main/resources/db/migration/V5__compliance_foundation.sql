-- Phase 5 compliance is opt-in per shop. Existing tenants remain disabled.
ALTER TABLE shops ADD COLUMN IF NOT EXISTS locality VARCHAR(100);
ALTER TABLE shops ADD COLUMN IF NOT EXISTS pincode VARCHAR(6);
ALTER TABLE shops ADD COLUMN IF NOT EXISTS e_invoice_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE shops ADD COLUMN IF NOT EXISTS e_way_bill_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE shops ADD COLUMN IF NOT EXISTS e_invoice_cancellation_window_hours INTEGER NOT NULL DEFAULT 24;

ALTER TABLE customers ADD COLUMN IF NOT EXISTS locality VARCHAR(100);
ALTER TABLE customers ADD COLUMN IF NOT EXISTS pincode VARCHAR(6);
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS compliance_locked BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS e_invoice_records (
    id BIGSERIAL PRIMARY KEY,
    shop_id BIGINT NOT NULL REFERENCES shops(id),
    invoice_id BIGINT NOT NULL REFERENCES invoices(id),
    status VARCHAR(30) NOT NULL,
    provider_reference VARCHAR(100),
    irn VARCHAR(100),
    acknowledgement_number VARCHAR(100),
    acknowledgement_date TIMESTAMP,
    signed_qr_data TEXT,
    request_payload TEXT NOT NULL,
    response_payload TEXT,
    last_error VARCHAR(1000),
    attempt_count INTEGER NOT NULL DEFAULT 0,
    generated_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_e_invoice_record_invoice UNIQUE(invoice_id),
    CONSTRAINT uk_e_invoice_record_shop_irn UNIQUE(shop_id, irn)
);

CREATE TABLE IF NOT EXISTS e_way_bill_records (
    id BIGSERIAL PRIMARY KEY,
    shop_id BIGINT NOT NULL REFERENCES shops(id),
    invoice_id BIGINT NOT NULL REFERENCES invoices(id),
    status VARCHAR(30) NOT NULL,
    e_way_bill_number VARCHAR(30),
    valid_until TIMESTAMP,
    transporter_id VARCHAR(20),
    transporter_name VARCHAR(100),
    transport_document_number VARCHAR(30),
    transport_document_date DATE,
    transport_mode VARCHAR(20),
    vehicle_number VARCHAR(20),
    vehicle_type VARCHAR(20),
    distance_km INTEGER,
    request_payload TEXT NOT NULL,
    response_payload TEXT,
    last_error VARCHAR(1000),
    attempt_count INTEGER NOT NULL DEFAULT 0,
    generated_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_e_way_bill_record_invoice UNIQUE(invoice_id),
    CONSTRAINT uk_e_way_bill_record_shop_number UNIQUE(shop_id, e_way_bill_number)
);

CREATE INDEX IF NOT EXISTS idx_e_invoice_records_shop_status_updated
    ON e_invoice_records(shop_id, status, updated_at);
CREATE INDEX IF NOT EXISTS idx_e_way_bill_records_shop_status_updated
    ON e_way_bill_records(shop_id, status, updated_at);
