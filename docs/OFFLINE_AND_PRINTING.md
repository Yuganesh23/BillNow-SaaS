# Offline invoices, PWA, and printing

The production frontend registers `sw.js` and can be installed as a PWA. The service worker caches only the application shell and same-origin static assets. It deliberately never caches `/api/` responses because those responses contain tenant data.

If invoice creation fails because the browser is offline or the network is unavailable, the draft is stored in IndexedDB with:

- the authenticated user and active branch;
- a temporary `OFF-...` reference;
- one stable idempotency key.

When connectivity returns, BillNow resubmits the invoice to its original branch. The backend idempotency constraint prevents a retry from creating a second invoice or reducing stock twice. Validation and stock errors become conflicts instead of being silently discarded. Offline invoices sync as unpaid; payments must be recorded after synchronization so an uncertain network response cannot duplicate a payment.

Invoice printing defaults to A4. Users can select 80 mm or 58 mm thermal layouts before opening the browser print dialog.
