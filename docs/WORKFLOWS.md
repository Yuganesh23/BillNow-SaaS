# Daily billing workflows

Phase 3 adds tenant-scoped workflow APIs and a lazy-loaded `/workflows` frontend page.

## Implemented

- Quotations, estimates, optional sales orders and delivery challans share one document model. They use the Phase 2 tax engine, have their own non-invoice series, validity/status, and do not change stock until conversion to an invoice.
- Conversion is idempotent: repeat conversion returns the already-linked invoice.
- Multiple partial payments can be allocated to one invoice. A payment larger than the remaining balance applies only the balance and stores the excess as customer credit.
- Customer aging uses live invoices and payment allocations and exposes 0–30, 31–60, 61–90 and 90+ day buckets.
- Payment receipts are available as tenant-scoped PDFs.
- UPI links use `upi://pay` with payee address/name, INR amount and invoice number. Invoice PDFs render the link as a QR when a shop UPI ID is configured.
- Recurring schedules support weekly, monthly and yearly runs. A unique `(schedule, scheduled date)` run record plus the invoice idempotency key prevents duplicates.
- Email and WhatsApp reminders are disabled by default. Set `BILLING_REMINDERS_ENABLED=true` only after provider setup. A per-day, per-channel unique reminder key prevents duplicate sends.
- Audit history is owner-only and tenant-scoped. Roles are `SHOP_OWNER`, `MANAGER`, `BILLER` and `VIEWER`; price changes, cancellations, recurring setup, audit and reports are permission-gated.
- Sales returns use Phase 2 credit notes with optional stock return. Existing purchase payment/ledger behavior remains tenant-scoped.

## Deliberate follow-ups

- Purchase-return debit notes need a purchase-linked line model before they should alter supplier balances or stock; the invoice-linked debit-note endpoint must not be reused for that purpose.
- Reminder text is currently provider-neutral; regulated template identifiers and local-language variants should be configured before enabling scheduled delivery.
- Audit rows capture actor, request path, method, status and time. Field-level before/after snapshots require a redaction design so customer PII and credentials are not copied into the audit table.
- The workflow page currently presents quotation state and receivable aging. Creation remains API-backed until the product/customer selectors are consolidated into reusable accessible form components.
