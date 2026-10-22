# XenBill production runbook

This runbook assumes Neon PostgreSQL, Railway for the Spring Boot API, and Vercel for the React frontend. Replace example hostnames and record the real project owners before launch.

## Service map

| Component | Source of truth | Health signal | Owner action |
| --- | --- | --- | --- |
| Frontend and `/api` proxy | Vercel project rooted at `frontend/` | landing page plus a proxied authenticated request | inspect Vercel deployment/function logs |
| API | Railway service built from root `Dockerfile` | `GET /actuator/health` returns `UP` | inspect Railway deploy/runtime logs and request ID |
| Database | Neon production branch | API readiness plus Neon compute/connection metrics | inspect Neon monitoring and branch history |
| Subscription payments | Razorpay account and webhook | signed webhook success and subscription state | inspect Razorpay event delivery; replay only by event ID |
| Email/WhatsApp/GST providers | configured provider accounts | application delivery/run records | inspect redacted provider response and retry controls |

## Deploy

1. Confirm CI is green for the exact commit and the worktree contains no secret files.
2. Confirm the target Neon branch and take a restorable checkpoint/branch before a migration-bearing release.
3. Review every new Flyway migration. Migrations are forward-only; never edit a migration already applied in production.
4. Deploy Railway first. Flyway must complete and Hibernate `ddl-auto=validate` must pass before Railway marks the release healthy.
5. Check `/actuator/health`, then make one authenticated read request. Record the Railway deployment ID, Git SHA and Flyway version.
6. Deploy Vercel from the same Git SHA. Confirm `BACKEND_ORIGIN`, then test registration/login, a proxied `/api` request, client-side route reload, security headers and PWA manifest.
7. Run the smoke checklist below. Do not enable a new provider or plan gate until its sandbox/production credentials and callback URLs are verified.

## Smoke checklist

- Register a disposable owner and complete shop onboarding.
- Create one product and customer, issue an invoice, download its PDF, record a partial payment and verify outstanding balance.
- Confirm a second shop cannot access the first shop's resource IDs.
- Generate and revoke a customer portal link.
- Export the shop JSON/ZIP and inspect that secrets/provider payloads are absent.
- If enabled: run Razorpay test payment/webhook, send test email/WhatsApp, and execute GSP/IRP sandbox validation.
- Confirm request IDs appear in both browser responses and Railway logs.

## Rollback

Application rollback and database rollback are separate decisions.

1. Stop user writes if the incident can corrupt tenant, invoice, stock or payment data.
2. Capture the failing deployment ID, Git SHA, request IDs and Flyway schema history before changing anything.
3. If the migration is additive and backward compatible, redeploy the last known-good Railway image/commit, then the matching Vercel deployment.
4. Never run `flyway repair`, delete schema-history rows, or manually reverse a migration during an incident without a reviewed database plan.
5. If the old binary cannot run against the new schema, roll forward with a corrective additive migration. Use Neon point-in-time restore/branching only after quantifying which writes would be lost.
6. Validate tenant counts, invoice totals, stock and recent payments after recovery. Document the decision and timestamps.

## Incident triage

1. Classify severity: security/data isolation, incorrect tax or money, unavailable checkout/API, delayed integration, or cosmetic.
2. For suspected cross-tenant access, payment duplication, secret exposure or tax corruption, disable the affected workflow and preserve logs immediately.
3. Correlate with `X-Request-Id`; do not paste JWTs, cookies, database URLs, OTPs, customer phone numbers or provider tokens into tickets/chat.
4. Check in order: Vercel proxy, Railway health/logs, recent deployment/migration, Neon connections/compute, then external provider status.
5. Communicate impact, start time, affected tenants/features, workaround and next update time. Do not claim data safety until queries prove it.
6. After recovery, write a blameless incident review with root cause, detection gap, corrective test and owner/date.

## Alerts required before launch

- Railway API readiness failure and restart loop.
- Sustained 5xx rate, authentication 429 spike and p95 latency threshold.
- Neon connection saturation, storage growth and long-running/slow queries.
- Razorpay webhook signature failures or repeated event-processing failures.
- Recurring invoice/reminder/compliance job failures.
- Frontend error-reporting increase after PII scrubbing is verified.

Exact thresholds and paging destinations are owner decisions and must be recorded in the production monitoring system. Email-only alerts without an on-call owner are not sufficient for payment or tenant-isolation incidents.

## Backup and restore drill

Before launch, perform this on non-production data using current Neon controls:

1. Record a known invoice count and checksum/total for a test shop.
2. Create a restore branch or point-in-time recovery target from before a controlled data change.
3. Connect a temporary API instance with all outbound integrations disabled.
4. Run Flyway validation and read-only checks against the restored branch.
5. Confirm tenant counts and the known invoice total, then destroy the temporary environment through the Neon console.
6. Record actual recovery point, recovery time, operator, date and evidence below.

Status: **not executed in this workspace**. Docker is unavailable locally and no production-like Neon restore operation was authorised. This is a launch blocker until an owner completes and records the drill.

## Contacts to fill before launch

- Technical/on-call owner:
- Database/Neon owner:
- Security incident contact:
- Customer communications owner:
- CA/GST reviewer:
- Legal/privacy reviewer:
- Razorpay, Meta/email and GSP/IRP account owners:
