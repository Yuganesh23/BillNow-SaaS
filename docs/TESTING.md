# Testing BillNow

## Database strategy

The normal integration suite uses H2 in PostgreSQL compatibility mode so it
can run on workstations without Docker. This is a fallback, not proof of real
PostgreSQL behavior.

`PostgresMigrationTest` uses PostgreSQL 16 through Testcontainers and applies
the Flyway migrations to a baseline schema. It is automatically skipped only
when no Docker-compatible container runtime is available. GitHub Actions
provides Docker and therefore runs this test on every pull request.

Run the complete backend verification with:

```shell
./mvnw verify
```

Run frontend verification with:

```shell
cd frontend
npm ci
npm run lint
npm run build
```

## Tenant fixtures

`TenantTestFixture` creates two isolated shops with owners, products,
customers, suppliers, and JWT-authenticated MockMvc request helpers. The
controller inventory test fails when a controller is added without being
classified, making tenant-isolation review an explicit part of endpoint work.

The current isolation suite exercises all ID-based tenant resources across
products, customers, suppliers, purchases, invoices, payments, inventory,
billers, shops, invoice PDFs, and WhatsApp invoice delivery. Aggregate and
account controllers are included in the inventory and rely on their active
shop context rather than accepting a resource ID.
