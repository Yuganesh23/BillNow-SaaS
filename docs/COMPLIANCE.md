# E-invoice and e-way-bill operations

BillNow contains a provider-neutral compliance workflow. It stores tenant-scoped attempts, prevents duplicate generation for an invoice, records provider responses, exposes failed jobs for manual retry, and prints generated IRN/acknowledgement/e-way identifiers on the invoice PDF.

## Safety defaults

- Both features are disabled for every existing and new shop.
- The built-in `mock` provider is for development and automated tests only.
- `COMPLIANCE_MOCK_ENABLED=false` in production prevents a shop owner from enabling either feature with the mock provider.
- Request and response bodies are stored for audit. A real adapter must redact credentials, tokens, and unrelated personal data before returning its audit response.

## Production provider work still required

Select an authorised GSP/IRP provider and obtain its sandbox credentials and API documentation. Implement a provider bean for `EInvoiceProvider`, selected by `COMPLIANCE_PROVIDER`, covering authentication/token caching, IRN generation/cancellation, e-way generation, vehicle update, cancellation, provider error mapping, timeouts and safe retries. Do not enable the shop flags until its sandbox certification checklist passes.

Provider credentials must be supplied as deployment secrets. Never put them in Git or expose them to Vite/browser variables.

## Sandbox checklist

1. Confirm seller GSTIN, legal name, address, locality, PIN and state code.
2. Confirm customer GSTIN, billing address, locality, PIN and state code.
3. Verify every item HSN/SAC, UQC, quantity, taxable value and backend-calculated GST.
4. Generate one IRN and verify its acknowledgement and signed QR against the provider portal.
5. Repeat the request and confirm the same BillNow record is returned without creating a second IRN.
6. Verify the downloaded PDF contains the IRN, acknowledgement and scannable signed-QR payload.
7. Exercise provider timeout/failure, then retry it from `/api/compliance/jobs`.
8. Cancel an IRN inside the configured window and confirm ordinary invoice cancellation remains blocked until the compliance state is reconciled.
9. Generate an e-way bill, update its vehicle, and cancel it inside the 24-hour window.
10. Attempt every read and mutation from a second shop and confirm it receives 404/403 without revealing the record.

The official references used to shape this boundary are the notified e-invoice schema and the NIC e-way-bill API documentation. Final payload mapping must use the selected provider's current sandbox specification.
