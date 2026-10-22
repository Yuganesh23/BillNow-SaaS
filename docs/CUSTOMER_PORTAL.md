# Customer invoice portal

BillNow can issue a short-lived link that lets one customer view and download only their own invoices.

## Security model

- Only a `SHOP_OWNER` can create or revoke a link.
- Creating a new link revokes all earlier active links for that customer in that shop.
- The random 256-bit token is returned once. Only its SHA-256 hash is stored.
- Every invoice lookup is constrained by both the token's `shop_id` and `customer_id`.
- Tokens expire and can be revoked immediately. Portal responses and PDFs use `Cache-Control: no-store`.
- The frontend moves the token from the URL to `sessionStorage` and removes it from browser history.

## Configuration

Set `FRONTEND_URL` on the backend to the public Vercel origin, for example `https://billnow.vercel.app`.
`CUSTOMER_PORTAL_ACCESS_HOURS` controls link lifetime and defaults to 72 hours.

No email or WhatsApp message is sent automatically. The owner copies the one-time displayed link and shares it using the channel agreed with the customer.
