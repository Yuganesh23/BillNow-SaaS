-- ADD COLUMN IF NOT EXISTS does not apply a default when a baseline schema
-- already contains the column (for example, a Hibernate-created test schema).
ALTER TABLE shops ALTER COLUMN e_invoice_enabled SET DEFAULT FALSE;
ALTER TABLE shops ALTER COLUMN e_way_bill_enabled SET DEFAULT FALSE;
ALTER TABLE shops ALTER COLUMN e_invoice_cancellation_window_hours SET DEFAULT 24;
ALTER TABLE invoices ALTER COLUMN compliance_locked SET DEFAULT FALSE;
