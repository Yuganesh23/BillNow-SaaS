# Plan limits

The public annual prices are BASE ₹4,999, PRO ₹9,999, and ENTERPRISE ₹24,999. Limits are enforced by the backend and returned by `GET /api/subscription/usage`.

Default packaging:

| Tier | Invoices/month | Active users | Branches | E-invoice |
|---|---:|---:|---:|---|
| Trial | 25 | 1 | 1 | No |
| Base | 250 | 2 | 1 | No |
| Pro | 2,000 | 10 | 5 | Yes |
| Enterprise | 100,000 | 100 | 100 | Yes |

Every value is environment-configurable with the `BILLING_LIMIT_*` variables in `application.properties`; treat the high enterprise values as negotiated caps, not a promise of unlimited capacity. Expired trials and subscriptions are rejected with HTTP 402. Idempotent invoice replays are checked before limits so a network retry can retrieve the invoice it already created.
