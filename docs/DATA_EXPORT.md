# Shop business-data export

Shop owners can download their active branch from Settings in either JSON or ZIP format. The ZIP contains a portable JSON snapshot plus separate UTF-8 CSV files for shop settings, users, products, customers, suppliers, invoices, invoice items, payments, purchases, purchase items, inventory movements, document notes and sequences, supplier ledger entries, quotations, recurring-invoice definitions and runs, reminder history, compliance records, and accounting-export settings.

Exports are generated only from repositories scoped to the authenticated shop. Password hashes, refresh tokens, WhatsApp access tokens, Razorpay secrets, invoice idempotency keys, and internal provider payloads are intentionally excluded. CSV values beginning with spreadsheet formula characters are escaped to prevent formula injection.

Authentication tokens, password-reset records, subscription webhook deliveries, request audit logs, and provider request/response payloads are operational security records rather than portable business data and are deliberately excluded. The download response is marked `no-store` and only the shop owner role can request it.

The JSON includes `formatVersion: 1`; future import or migration tools must branch on this value instead of assuming the shape never changes.
