# Changelog

All notable XenBill changes are recorded here. The project has not yet published a production release.

## Unreleased

### Added

- GST tax engine, compliant document metadata and additive Flyway migrations.
- Quotations, recurring invoices, reminders, aging, reports and exports.
- E-invoice/e-way bill provider abstractions and sandbox workflows.
- Guided onboarding, atomic CSV imports, PWA/offline queue and thermal print layouts.
- English, Tamil and Hindi primary-flow localization.
- Razorpay plan limits and idempotent webhook handling.
- Shop JSON/ZIP export and a secure customer invoice portal.
- Professional XenBill landing page, SEO metadata, draft Privacy/Terms pages and security headers.
- Authenticated OpenAPI contract and Swagger UI.
- Subscription cancellation at period end, plan upgrades/downgrades, billing history and GST tax invoices for subscription payments.

### Fixed

- Cancelled subscriptions and failed renewals no longer lock shops out of their customers and invoices; lapsed shops keep read-only access.
- Subscription and invoice-entitlement errors return specific 4xx codes instead of HTTP 500.

### Security

- Razorpay webhooks with an invalid signature are rejected; previously the verification result was ignored.
- Tenant-scoped access tests, hardened auth cookies, HMAC password-reset tokens, rate limiting, secret validation, request IDs and PII-scrubbed error reporting.

### Known launch blockers

- See `docs/LAUNCH_CHECKLIST.md` for external credentials, legal/licensing, CA review, restore/load testing, account deletion and operational ownership gaps.
