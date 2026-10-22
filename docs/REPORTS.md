# Reports and exports

## Core rule

Reports are derived from invoices, purchases, notes, payments, supplier ledgers and stock movements. Report totals are never stored separately, so they cannot drift from the documents that produced them. Every query starts with the active `shop_id` and date range.

## Definitions

- Sales register: invoices, including cancelled documents, with the backend-calculated taxable value and tax breakup. The UI and exports support party, tax rate and document-type filters.
- Purchase register: purchases with persisted CGST, SGST, IGST and cess. V4 backfills old aggregate purchase tax using supplier/shop state; historical cess cannot be reconstructed and is zero.
- Note register: credit and debit notes linked to the original invoice. Current notes store a gross amount, so report tax components are apportioned using the original invoice ratios.
- Payment register: customer receipts by method and invoice. Day book combines customer receipts and supplier-ledger entries. Both APIs and the UI are paginated so large histories are not loaded into the browser at once.
- GSTR-1 style summary: B2B, B2C large, B2C small, credit/debit notes, HSN and document-series summaries. It is a review aid, not a filing-ready claim.
- GSTR-3B style summary: outward liability less credit notes, recorded inward purchase tax as eligible ITC, and their arithmetic difference. Eligibility, blocked credits, reverse charge, exports and SEZ treatment are not automated.
- Stock valuation: current quantity multiplied by weighted-average recorded purchase cost. Products without purchase history use their current purchase price.
- Profit and loss: gross invoice sales less credit notes and purchases. Expenses are not modelled, so this is not statutory net profit.
- Receivables/payables: document value less allocated payments in the selected period.

## Exports

Registers are downloadable as CSV and Excel. GST summaries are downloadable as Excel. Tally XML exports sales and purchase vouchers using per-shop ledger mappings. Owners and managers can edit those mappings under **Reports → Export setup**. Tally export is beta and must be imported into a test company before production use.

## CA review required

- `REPORTS_GST_B2CL_THRESHOLD` controls the B2C-large classification threshold and defaults to `250000.00`. Confirm its current value and applicability with a CA.
- Confirm whether proportional note-tax allocation is acceptable until itemised credit/debit notes are implemented.
- Confirm ITC eligibility and exclusions. BillNow reports recorded purchase tax; it does not decide legal eligibility.
- Confirm reverse-charge treatment, HSN aggregation and Tally ledger mappings.

## Performance

`PostgresMigrationTest` creates 10,000 invoices on PostgreSQL, runs `EXPLAIN (ANALYZE, BUFFERS)` for the primary tenant/date register query, prints `REPORT_PERFORMANCE_10K_MS`, and fails above one second. It runs in CI where Docker is available and is skipped locally without Docker. V4 adds composite tenant/date/status and join indexes.

Production-like performance still needs verification on a disposable Neon branch. Use a direct connection for Flyway and the pooled endpoint for application traffic. Never run the seed against production.

## Known accounting gaps

- Credit/debit notes still store only a gross amount. Their tax breakup is proportional to the source invoice until itemised note lines are added and CA-reviewed.
- Expenses are not modelled. The business summary deliberately says “sales less purchases” and must not be presented as statutory net profit.
