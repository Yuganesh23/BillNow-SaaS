-- Phase 4 reports are derived from source documents. These indexes support the
-- tenant/date access path used by every register and summary query.
ALTER TABLE purchases ADD COLUMN IF NOT EXISTS taxable_amount NUMERIC(12,2) NOT NULL DEFAULT 0.00;
ALTER TABLE purchases ADD COLUMN IF NOT EXISTS cgst_total NUMERIC(12,2) NOT NULL DEFAULT 0.00;
ALTER TABLE purchases ADD COLUMN IF NOT EXISTS sgst_total NUMERIC(12,2) NOT NULL DEFAULT 0.00;
ALTER TABLE purchases ADD COLUMN IF NOT EXISTS igst_total NUMERIC(12,2) NOT NULL DEFAULT 0.00;
ALTER TABLE purchases ADD COLUMN IF NOT EXISTS cess_total NUMERIC(12,2) NOT NULL DEFAULT 0.00;

-- Historical purchase rows only retained aggregate tax. Preserve that value
-- conservatively by state; cess cannot be reconstructed and remains zero.
UPDATE purchases p
SET taxable_amount = COALESCE(p.subtotal, 0),
    cgst_total = CASE WHEN COALESCE(s.state_code, sh.state_code) = sh.state_code
                      THEN ROUND(COALESCE(p.tax_amount, 0) / 2, 2) ELSE 0 END,
    sgst_total = CASE WHEN COALESCE(s.state_code, sh.state_code) = sh.state_code
                      THEN COALESCE(p.tax_amount, 0) - ROUND(COALESCE(p.tax_amount, 0) / 2, 2) ELSE 0 END,
    igst_total = CASE WHEN COALESCE(s.state_code, sh.state_code) <> sh.state_code
                      THEN COALESCE(p.tax_amount, 0) ELSE 0 END
FROM suppliers s, shops sh
WHERE p.supplier_id = s.id AND p.shop_id = sh.id
  AND p.taxable_amount = 0 AND COALESCE(p.subtotal, 0) <> 0;

CREATE INDEX IF NOT EXISTS idx_invoices_shop_created_status
    ON invoices(shop_id, created_at, status);
CREATE INDEX IF NOT EXISTS idx_invoices_shop_customer_created
    ON invoices(shop_id, customer_id, created_at);
CREATE INDEX IF NOT EXISTS idx_invoice_items_invoice_product
    ON invoice_items(invoice_id, product_id);
CREATE INDEX IF NOT EXISTS idx_purchases_shop_created_status
    ON purchases(shop_id, created_at, status);
CREATE INDEX IF NOT EXISTS idx_purchases_shop_supplier_created
    ON purchases(shop_id, supplier_id, created_at);
CREATE INDEX IF NOT EXISTS idx_purchase_items_purchase_product
    ON purchase_items(purchase_id, product_id);
CREATE INDEX IF NOT EXISTS idx_document_notes_shop_created_type
    ON document_notes(shop_id, created_at, note_type);
CREATE INDEX IF NOT EXISTS idx_inventory_movements_shop_product_created
    ON inventory_movements(shop_id, product_id, created_at);
CREATE INDEX IF NOT EXISTS idx_supplier_ledger_shop_created
    ON supplier_ledger(shop_id, transaction_date);

CREATE TABLE IF NOT EXISTS report_export_settings (
    id BIGSERIAL PRIMARY KEY,
    shop_id BIGINT NOT NULL REFERENCES shops(id),
    sales_ledger VARCHAR(100) NOT NULL DEFAULT 'Sales',
    purchase_ledger VARCHAR(100) NOT NULL DEFAULT 'Purchases',
    cgst_ledger VARCHAR(100) NOT NULL DEFAULT 'Output CGST',
    sgst_ledger VARCHAR(100) NOT NULL DEFAULT 'Output SGST',
    igst_ledger VARCHAR(100) NOT NULL DEFAULT 'Output IGST',
    cess_ledger VARCHAR(100) NOT NULL DEFAULT 'Output Cess',
    CONSTRAINT uk_report_export_settings_shop UNIQUE(shop_id)
);
