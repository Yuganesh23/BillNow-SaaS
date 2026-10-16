# Phase 1 cleanup notes

## Removed

- Empty duplicate security stubs: `security/CustomUserDetailsService`,
  `security/JwtAuthenticationFilter`, and `security/JwtService`. The live JWT
  implementation remains under `auth/`; `SecretStringCipher` remains under
  `security/` because WhatsApp credentials use it.
- Unused frontend maintenance scripts: `fix.js`, `fix.cjs`, `test_api.js`, and
  `update_register.cjs`.
- The unused frontend mock store at `src/mock/store.js`.
- Unused frontend imports, variables, and catch bindings reported by Oxlint.

The frontend lint command now uses `--deny-warnings`, so any future warning
fails local verification and CI. Experimental React compiler diagnostics that
are not part of the current runtime contract are explicitly disabled in
`.oxlintrc.json`; rules-of-hooks remains an error.

## Intentionally retained

- `.agents/` and `skills-lock.json` are tracked intentionally. They were
  installed for this project through the Neon CLI at the owner's request and
  provide the checked-in Neon agent workflow. They are therefore not treated
  as disposable local Codex state.
- `security/SecretStringCipher.java` is used by both the WhatsApp configuration
  service and sender.
- H2 remains a test dependency as the documented local fallback because Docker
  is unavailable on the current workstation. CI runs the PostgreSQL
  Testcontainers migration test against PostgreSQL 16.

## Deferred rename plan

Renames are intentionally deferred to a cleanup-only phase because they touch
many imports and should not be mixed with behavior changes.

| Current name | Proposed name |
| --- | --- |
| `User_entity` | `User` |
| `Shop_entity` | `Shop` |
| `Customer_entity` | `Customer` |
| `Product_entity` | `Product` |
| `Invoice_entity` | `Invoice` |
| `Payment_entity` | `Payment` |
| `Purchase_entity` | `Purchase` |
| `PurchaseItem_entity` | `PurchaseItem` |
| `Supplier_entity` | `Supplier` |
| `SupplierLedger_entity` | `SupplierLedgerEntry` |
| `Inventory_entity` | `InventoryMovement` |
| `SaaSSubscription_entity` | `SaaSSubscription` |
| `RefreshToken_entity` | `RefreshToken` |
| `PasswordResetToken_entity` | `PasswordResetToken` |
| `AuditLog_entity` | `AuditLog` |
| `userRepository` | `UserRepository` |
| `response.java` and generic response classes | Domain-specific response DTO names |

Before renaming entities, verify that no JPQL query depends on the implicit
entity class name. Keep database table and column names unchanged during the
Java-only rename.

## Follow-up risks

- Several older services still use `RuntimeException` for validation and
  authorization errors. Tenant-resource not-found paths exercised by the new
  isolation suite now return `AppException`, but the remaining cases should be
  migrated incrementally with behavior tests.
- The PostgreSQL migration test baselines a schema created from the current
  entity model before applying additive migrations. A future migration-only
  baseline should make a brand-new database reproducible without Hibernate
  schema generation.
- Test output reports duplicate `org.json.JSONObject` implementations from
  transitive test dependencies. Resolve the dependency convergence before
  code starts relying directly on that class.
