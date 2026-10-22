# BillNow security notes

## Phase 0 changes

- Purchase creation now loads every product with `findByIdAndShopId`. A foreign product is reported as not found and the transaction rolls back the purchase and all earlier stock changes.
- Branch selection, shop updates/deletes, biller mutations, and internal WhatsApp invoice status updates use shop-scoped repository queries. The remaining bare `findById` calls resolve the currently authenticated user or refresh-token owner, not tenant-owned business records.
- Production cookies default to `SameSite=Lax`. `Secure` remains enabled by default in the production profile.
- JWT, Razorpay, WhatsApp token-encryption, and password-reset HMAC secrets have no usable source-code defaults. The production profile fails during startup when a required secret is missing.
- Password-reset OTPs are stored as HMAC-SHA-256 values keyed with `OTP_HMAC_SECRET` (falling back to the required JWT secret only when an explicit OTP secret is not supplied). Comparisons remain constant-time, OTPs expire after five minutes, and attempts remain capped at five.
- Login is limited per IP and email. Forgot-password and reset-password are limited per IP and email. Registration is limited per IP. Limits and the common time window are configurable through the `AUTH_RATE_LIMIT_*` environment variables. Rejections use the normal `ApiError` body, HTTP 429, and `Retry-After`.
- The unauthenticated `/api/reports/debug6` endpoint was removed.
- Vercel no longer routes API traffic to the stale Render deployment. The filesystem API function proxies `/api/*` to `BACKEND_ORIGIN`; the remaining rewrite is only the SPA fallback.
- Unexpected runtime exception messages are logged server-side and replaced with a generic HTTP 500 response so database, provider, or credential details are not returned to clients. Expected errors added or changed in this phase use `AppException`.

## Rate-limit configuration

| Variable | Default |
| --- | ---: |
| `AUTH_RATE_LIMIT_WINDOW_SECONDS` | 60 |
| `AUTH_RATE_LIMIT_LOGIN_IP_MAX` | 20 |
| `AUTH_RATE_LIMIT_LOGIN_EMAIL_MAX` | 10 |
| `AUTH_RATE_LIMIT_FORGOT_IP_MAX` | 10 |
| `AUTH_RATE_LIMIT_FORGOT_EMAIL_MAX` | 3 |
| `AUTH_RATE_LIMIT_RESET_IP_MAX` | 10 |
| `AUTH_RATE_LIMIT_RESET_EMAIL_MAX` | 5 |
| `AUTH_RATE_LIMIT_REGISTER_IP_MAX` | 5 |

## Residual risks

- Rate-limit counters are process-local and reset on deployment. They do not coordinate across multiple Railway replicas. Use Redis or an edge/WAF limiter before horizontal scaling.
- Numerous legacy services still throw bare `RuntimeException` for business validation. The global handler now prevents message disclosure, but those sites should be migrated to specific `AppException` codes and statuses incrementally.
- CSRF is disabled because authentication is stateless, but credentials are cookies. Keep `SameSite=Lax` or `Strict`, validate deployment origins, and do not change production cookies to `SameSite=None` without adding explicit CSRF protection.
- OTP HMAC key rotation invalidates outstanding password-reset codes. Rotate it during a maintenance window.
- Login and password-reset limits reduce automated abuse but do not replace Railway/Vercel edge protection, bot detection, or account monitoring.
- The application still needs a full endpoint-by-endpoint authorization matrix and systematic cross-tenant suite; that is Phase 1 scope.
- Dependency scanning, backup/restore validation, and a complete incident runbook remain Phase 7 work.

## Browser security headers

The API adds these headers to every response: a deny-by-default API Content Security Policy, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: no-referrer`, and a restrictive Permissions Policy. HSTS is emitted for secure requests; `server.forward-headers-strategy=framework` lets Railway's forwarded HTTPS scheme drive that decision.

Vercel applies a frontend CSP and the corresponding HSTS, frame, MIME, referrer, and permissions headers to all routes. The CSP permits XenBill's own bundles, HTTPS images and API/error-report connections, the configured service worker, and Razorpay Checkout's script/frame origins. Any new third-party script, font, frame, or worker must receive a CSP review before it is added.

The header sets are covered by `SecurityHeadersFilterTest` and `frontend/src/securityHeaders.test.js`. Production headers must still be confirmed with `curl -I` against the final Vercel and Railway domains because a hosting layer can modify or replace application headers.
