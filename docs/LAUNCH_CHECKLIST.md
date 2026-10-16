# XenBill public launch checklist

Last reviewed against repository state: 16 October 2026. A checked item means repository evidence exists; it does not substitute for production verification.

## Code and data safety

- [x] Tenant-scoped repository patterns and cross-tenant controller tests are present.
- [x] Backend tax calculations use `BigDecimal`; GST assumptions and CA questions are documented.
- [x] Production uses Flyway plus `ddl-auto=validate`; migrations are additive through V8.
- [x] JWT/refresh-cookie hardening, password-reset HMAC, auth rate limiting and generic unexpected-error responses are implemented.
- [x] Vercel and API security headers are configured and tested.
- [x] Shop data export excludes credentials and provider payloads.
- [ ] In-product account deletion with a grace period is not implemented.
- [ ] A production-like Neon backup/restore drill has not been executed; follow `docs/RUNBOOK.md`.
- [ ] Resolve and document the statutory retention schedule before implementing irreversible deletion.

## Product capability

- [x] Billing, inventory, purchases, payments, core reports and PDFs are implemented.
- [x] GST tax engine, document numbering, credit/debit notes, quotations, recurring invoices, reminders and customer aging have tested backend workflows.
- [x] CSV import preview/atomic commit and JSON/ZIP shop exports are implemented.
- [x] PWA shell, offline invoice queue, idempotent sync and 58/80 mm printing are implemented.
- [x] English, Tamil and Hindi cover primary flows; advanced screens remain English-first.
- [x] Customer portal links are short-lived, revocable, hashed at rest and tenant/customer scoped.
- [x] Authenticated OpenAPI JSON and Swagger UI are generated on the backend.
- [ ] E-invoice/e-way bill production use is blocked on an authorised GSP/IRP selection, credentials and sandbox certification.
- [ ] Email, WhatsApp, object storage and error reporting each require production account/configuration checks.
- [ ] Native Android/iOS apps are not included; the supported mobile delivery is the installable responsive PWA.
- [ ] Full Playwright launch journeys and recorded Lighthouse scores are not yet present.

## Commercial and support

- [x] Annual Base, Pro and Enterprise prices and configurable plan limits are displayed.
- [x] Razorpay webhook processing is signature-checked and idempotent.
- [ ] Final renewal, cancellation, refund, failed-payment and downgrade policy requires owner/legal approval.
- [ ] Real support WhatsApp number, support hours, escalation targets and customer SLA are not supplied.
- [ ] Replace illustrative marketing scenarios with approved customer proof before presenting them as testimonials.
- [ ] Confirm plan IDs, webhook URL/secret, live-mode payment flow, GST on subscription invoices and refund workflow.

## Legal and compliance

- [x] Public `/privacy` and `/terms` draft pages exist and explicitly disclose unresolved decisions.
- [ ] Legal entity name, address, grievance officer and formal notice contact are missing.
- [ ] Indian counsel must review DPDP, contracts, cross-border processing, liability and messaging consent.
- [ ] A CA must approve `docs/GST_ASSUMPTIONS.md`, current thresholds and filing/export interpretations.
- [ ] Repository license/fork ownership is unresolved; do not sell or redistribute until the owner confirms rights.
- [ ] Third-party notices have not been generated and reviewed.

## Operations and security verification

- [x] Health endpoint, request IDs, PII-scrubbed optional frontend reporting and structured log context exist.
- [x] Deployment, rollback, incident and restore procedures are documented in `docs/RUNBOOK.md`.
- [ ] Configure production alerts and named on-call owners.
- [ ] Run dependency/security scans and resolve or assign every high/critical finding.
- [ ] Run a realistic invoice-creation load test against a non-production Neon branch and record p50/p95/error rate.
- [ ] Verify final Vercel/Railway response headers, cookie flags, CORS origins and CSP in production.
- [ ] Execute full smoke tests after deploying the exact release SHA.

## Current automated evidence

- Backend: `./mvnw verify` passed on 16 October 2026 with 66 tests, 0 failures, and one skipped PostgreSQL Testcontainers migration test because Docker was unavailable.
- Frontend: `npm ci`, lint, seven Node tests and production build passed on 16 October 2026.
- The real PostgreSQL Flyway migration suite, production headers, provider sandboxes, restore drill, load test and end-to-end browser journeys remain unverified.

## Launch decision

**Not ready for an unrestricted public launch.** A controlled private pilot can be considered only after production secrets/domains are configured, CA and legal review are complete, live payment/provider smoke tests pass, backups are proven, monitoring has an owner, and the licensing question is resolved.
