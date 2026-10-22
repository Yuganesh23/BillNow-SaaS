# Data imports

BillNow accepts CSV and XLSX imports for products, customers, suppliers, and opening balances. Download the matching template from the Imports page so field names remain stable.

Imports have two stages. Preview parses and validates every row without changing data. Commit repeats the same validation inside one database transaction; if any row is invalid, nothing is written.

Opening-balance semantics:

- `CUSTOMER`: `identifier` is the customer's WhatsApp number and a positive `amount` is money the customer owes the shop.
- `SUPPLIER`: `identifier` is the exact supplier business name and a positive `amount` is money the shop owes the supplier.

Each batch is limited to 5,000 data rows. Money values accept at most two decimal places and are stored using `BigDecimal` with `HALF_UP` rounding.
