# XenBill API reference

The backend generates an OpenAPI 3 contract with springdoc 3.x, the line compatible with Spring Boot 4.

- Interactive reference: `/swagger-ui.html`
- JSON contract: `/v3/api-docs`

Both routes require a valid XenBill session. They are served by the Railway backend domain, not the Vercel SPA proxy. Sign in against that same backend origin before using the interactive page, or request the JSON contract with a valid bearer token. Most tenant operations also use `X-Branch-Id` to select a shop owned by the current user.

The contract is generated from controllers and DTO validation at runtime. It is a discoverability aid, not a version-stability promise. Before offering a third-party public API, add explicit API versioning, scoped API keys or OAuth, per-client rate limits, webhook signing, an external developer agreement and compatibility tests.

The `cookieAuth` scheme documents the browser session cookie. Because it is HttpOnly, callers do not read or copy it; the browser attaches it automatically. Public auth, health, Razorpay webhook and customer-portal routes have their own security rules in code and should be checked against the endpoint-permission matrix before every release.
