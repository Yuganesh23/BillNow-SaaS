# BillNow: Agent Instructions

## Product
BillNow is a multi-tenant GST billing SaaS for Indian small businesses (invoicing, inventory,
purchases, payments, reports). Correctness and tenant isolation matter more than speed or features.

## Stack
- Backend: Spring Boot 4, Java 17, Maven (use ./mvnw), package com.example.Billing
- DB: PostgreSQL (Neon). Flyway migrations in src/main/resources/db/migration. Hibernate ddl-auto=validate in prod.
- Frontend: React 19 + Vite in /frontend, plain JS/JSX, axios client in frontend/src/api
- Deploy: Railway (API, Dockerfile), Vercel (frontend + /api proxy at frontend/api/[...path].js)
- Auth: JWT + refresh token in HttpOnly cookies, stateless, CSRF disabled (so cookie SameSite must stay Lax or Strict)

## Hard rules
1. Multi-tenancy: every query touching tenant data MUST be scoped by shop id (findByIdAndShopId style). Never use bare findById on tenant entities.
2. Money: BigDecimal only, scale 2, RoundingMode.HALF_UP. Never double/float.
3. Migrations: never edit an existing migration. Add V2, V3, ... Additive and backward compatible. Keep spring.jpa.hibernate.ddl-auto=validate.
4. No secrets in code or defaults. Production config must fail at startup if a required secret is missing.
5. Throw AppException (with HttpStatus and code) for expected errors, never bare RuntimeException.
6. Every behavior change ships with tests. Every tenant-scoped endpoint gets a cross-tenant test (shop A cannot read or modify shop B).
7. Do not rename or mass-reformat files in the same commit as behavior changes. Put cleanup in its own commit.
8. Backend is the single source of truth for tax math. The frontend may preview, but must display backend-calculated values.
9. Do not invent GST rules. If a rule is uncertain, make it configurable and note it in docs/GST_ASSUMPTIONS.md for a CA to review.

## Workflow
- Before coding: read the relevant files, then state a short plan.
- Run ./mvnw verify and, in /frontend, npm ci && npm run lint && npm run build before finishing.
- If dependencies cannot be downloaded or tests cannot run in your sandbox, STOP and report exactly what failed. Do not claim success.
- Commit in small logical commits with clear messages. Summarize what changed, what you could not verify, and any follow-ups.
