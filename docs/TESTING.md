# Testing

```bash
./mvnw verify                  # Java integration tests and the three runnable jars
node --test tools/frontend_test.mjs # frontend controls and API timeout behaviour
./mvnw -pl choco-core test     # one module
./mvnw -pl choco-portal test -Dtest=CheckoutTest
python tools/smoke_test.py     # end-to-end against running services (see README)
```

Each Spring test context gets its own in-memory database (`jdbc:h2:mem:${random.uuid}`), so classes cannot interfere, and tests create
uniquely named users and products instead of cleaning up.

## What is covered

| Module | Tests | Focus |
|---|---|---|
| choco-model | 5 | Flyway + `ddl-auto=validate` (entities match migrations); every catalog of the guide loaded with the exact row counts; launch-tier visibility; seeded permissions |
| choco-core | 41 | **Stock** (reserve/sell/release, never oversold with 30 concurrent buyers for 10 units, ledger, stock-take limits); **order lifecycle** (pay, idempotent pay, cancel, refund, timeouts, auto-complete, invalid transitions, pickup vs delivery); **loyalty** (earning, levels, redemption cap, insufficient balance); **payments** (success, decline, retry, cash confirmation, ownership); **personalisation** (quote, versioned proofs, deposit only after approval, logo permission, refunds); **experiences** (seat limits with 12 concurrent bookings for 3 seats, cancellation notice, session cancellation, unpaid release); state machine and box expansion unit tests |
| choco-admin | 44 | **Permissions** (role-based access, immediate effect of disabling a role, custom roles, super-admin protection, lockout, token invalidation, password rules); **catalog** (creation rules, publishing gates, claim verification, brand workflow, protected reference data, soft delete); **operations** (stock rules, batch release controls, preorder windows, sessions, bundles, playbook); **orders** (workflow, refunds, returns with refund ceiling, settings); **members, coupons, content, reports** (contribution arithmetic) |
| choco-portal | 40 | **Auth** (registration with codes, lockout, token audience separation); **catalog** (public browsing, draft products hidden, allergen exclusion, filters, sorting, verified claims); **checkout** (SKU pricing, coupons and minimum spend, points cap, delivery and gift fees, bundles, pickup lead time, chilled-item rule, preorder capacity and cut-off, payment decline and success, ownership); **member centre**; **build-a-box**; **16 customers racing for 5 units over HTTP: exactly 5 orders succeed** |
| choco-search | 7 | database engine: keyword, filters, sorting, allergen exclusion, facets, recommendations, index-key protection |

## Bugs the tests found during development

* Product creation returned stale stock because atomic stock updates bypass the persistence context (fixed by refreshing before reading).
* The returns search query referenced a parameter that was never bound, which made the dashboard fail (found by the end-to-end run, then pinned by the report tests).
* Two configuration files each defined `logging:` after an automated edit, which prevented the back office from starting.
* The pay-by-card failure was reported as HTTP 500; it is now `402 Payment Required`.

## What is not covered

* The Elasticsearch engine, RabbitMQ topology, Redis-backed store and cache, MinIO storage and MySQL are exercised only by compilation and by
  their H2/in-memory counterparts (see [DEPLOYMENT.md](DEPLOYMENT.md)). Testcontainers-based tests would close this gap.
* A fully automated browser regression suite is not included. The storefront and staff dashboard require visual and keyboard
  checks in addition to the shared frontend tests. See [FRONTEND.md](FRONTEND.md).
* Load and soak tests.
