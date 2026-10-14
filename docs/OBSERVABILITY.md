# Observability and cold starts

Every backend response includes `X-Request-Id`. Safe incoming IDs are preserved and generated IDs are added to the logging context. Include this ID in support reports to correlate browser failures with Railway logs.

Frontend error delivery is opt-in through `VITE_ERROR_REPORTING_URL`. Before transmission, common credential and customer identifier fields are redacted. Do not point this variable at an analytics service until its retention, India data handling, and data-processing terms have been reviewed.

Railway may need time to start an idle service. Vercel should keep `/api` proxy timeouts above the measured cold-start duration. The UI retains its loading state while requests are pending; production monitoring should alert on readiness failures and sustained 5xx rates rather than a single slow request.
