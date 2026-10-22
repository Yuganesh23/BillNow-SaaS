# Subscription billing

How BillNow charges shops for plans through Razorpay Subscriptions. Shop-to-customer invoicing is documented elsewhere.

## Lifecycle

| Step | Endpoint / event | Effect |
|---|---|---|
| Subscribe | `POST /api/subscription/create`, Razorpay Checkout, `POST /api/subscription/verify` | Tier and `subscriptionEndsAt` are set once the checkout signature is verified. |
| Renewal | `subscription.charged` webhook | Period dates move forward, a billing record and GST tax invoice are created. |
| Upgrade | `POST /api/subscription/change-plan` with a higher plan | Razorpay is asked to change the plan **now**. The new tier is granted only when a webhook reports the new `plan_id`. |
| Downgrade | `POST /api/subscription/change-plan` with a lower plan | Scheduled for the **end of the paid cycle**. Refused with `409 PLAN_DOWNGRADE_BLOCKED` while active users or branches exceed the target plan. |
| Undo a scheduled change | `POST /api/subscription/change-plan/cancel` | Razorpay's pending update is cancelled. |
| Cancel | `POST /api/subscription/cancel` | Razorpay stops renewing at the end of the paid cycle (`cancel_at_cycle_end`). Access continues until then; `subscription.cancelled` arrives at the end. A scheduled cancellation cannot be undone; the owner subscribes again after it ends. |
| Failed renewal | `subscription.pending` / `subscription.halted` | Access ends at the paid-through date plus `BILLING_FAILED_PAYMENT_GRACE_DAYS` (default 0). A later successful charge restores it. |
| History | `GET /api/subscription/charges`, `GET /api/subscription/charges/{id}/invoice` | Shop-scoped list of payments and the tax invoice PDF. |

All subscription endpoints are limited to the shop owner and act on the active branch only.

## Access rules

- Plan access is decided only by `shops.subscription_tier` and `subscription_ends_at` (or `trial_ends_at` on the trial), enforced by `PlanLimitsService`.
- A lapsed shop keeps **read-only** access: invoices, customers and reports stay visible; creating or cancelling invoices returns `402 TRIAL_EXPIRED` or `402 SUBSCRIPTION_EXPIRED`.
- Subscription events never change `shops.active`. Migration V9 re-enabled shops that the previous behaviour had disabled.

## Razorpay setup

Enable these webhook events on the endpoint `/api/webhooks/razorpay` with `RAZORPAY_WEBHOOK_SECRET`:

`subscription.authenticated`, `subscription.activated`, `subscription.charged`, `subscription.updated`, `subscription.pending`, `subscription.halted`, `subscription.cancelled`, `subscription.completed`, `subscription.expired`, `subscription.resumed`.

Requests whose `X-Razorpay-Signature` does not match are rejected with `400 INVALID_WEBHOOK_SIGNATURE`. Events are processed once per event id.

Verify in Razorpay test mode before launch:

- Razorpay supports plan updates only for some payment methods (for example card mandates). If an update is refused, the API returns `502 PAYMENT_PROVIDER_ERROR`; the owner can cancel and subscribe again.
- How much Razorpay charges for an immediate upgrade, and whether it prorates, is decided by Razorpay. Confirm the amount and refund policy before publishing upgrade terms.

## Tax invoices

Configure BillNow's own GST details:

| Variable | Purpose |
|---|---|
| `BILLING_SELLER_LEGAL_NAME` | Supplier legal name printed on the invoice |
| `BILLING_SELLER_GSTIN` | Supplier GSTIN; its first two digits are the supplier state |
| `BILLING_SELLER_ADDRESS` | Supplier address |
| `BILLING_SAAS_SAC_CODE` | SAC for the subscription service (to be confirmed by a CA) |
| `BILLING_SAAS_GST_RATE` | GST rate in percent, default `18.00` (to be confirmed by a CA) |
| `BILLING_SAAS_INVOICE_PREFIX` | Invoice number prefix, default `BN` (numbers look like `BN/2026-27/00001`, max 16 characters) |

Payments are always recorded. Until legal name, a valid GSTIN and SAC are set, no invoice number is issued and the download returns `409 BILLING_ENTITY_NOT_CONFIGURED`; the invoice is issued on the first download after configuration. The calculation rules are listed in `docs/GST_ASSUMPTIONS.md`.
