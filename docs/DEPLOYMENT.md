# Deployment

## Profiles

| Profile | Use | Infrastructure | Data |
|---|---|---|---|
| `dev` (default) | local development, demos | none: H2 file database in `~/.chocomall`, Caffeine cache, in-memory codes, local files, database search | guide catalogs + demo shop + demo accounts |
| `test` | automated tests | none: a fresh in-memory H2 per test context | demo shop for most suites |
| `prod` | real deployments | MySQL 8, Redis, RabbitMQ, MinIO, Elasticsearch | guide reference catalogs only (brands, flavors, types, playbook, guides); no products, no accounts except the optional first admin |

Select with `SPRING_PROFILES_ACTIVE=prod` (the Docker images set it).

## Docker Compose

```bash
cp .env.example .env
#   fill every value; generate secrets with:  openssl rand -base64 48
docker compose up --build
```

Start-up order is enforced with health checks: MySQL, Redis, RabbitMQ, MinIO, Elasticsearch → **admin** (runs the Flyway migrations and
creates the first super admin) → portal and search.

| Service | Port | Health |
|---|---|---|
| choco-admin | 8080 | `/actuator/health` |
| choco-portal | 8085 | `/actuator/health` |
| choco-search | 8081 | `/actuator/health` |
| RabbitMQ management | 127.0.0.1:15672 | |
| MinIO console | 127.0.0.1:9001 | |

After the first start: sign in to the back office with `ADMIN_USERNAME`/`ADMIN_PASSWORD`, change the password, create staff accounts, then
add products (the production profile deliberately loads none), set the shop address and rules under *Orders > Settings*, and call
`POST /search/index/importAll` (with the `X-Search-Admin-Key` header) once to build the Elasticsearch index.

## Configuration reference (production)

| Variable | Required | Meaning |
|---|---|---|
| `JWT_SECRET` | yes | Base64 HMAC key, at least 32 bytes. Rotating it signs everyone out. |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | yes (password) | MySQL connection. |
| `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` | yes | cache and short-lived codes. |
| `RABBIT_HOST`, `RABBIT_PORT`, `RABBIT_USERNAME`, `RABBIT_PASSWORD` | yes | delayed order cancellation, search sync. |
| `MINIO_ENDPOINT`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`, `MINIO_BUCKET` | yes | product images. |
| `ELASTICSEARCH_URIS` | search only | e.g. `http://elasticsearch:9200`. |
| `SEARCH_ADMIN_KEY` | search only | protects `/search/index/**`; leave empty to disable index management. |
| `ADMIN_USERNAME`, `ADMIN_PASSWORD` | first start | creates the first super admin when none exists. Empty = no bootstrap. |
| `CORS_ORIGINS` | if the front end is on another origin | comma separated list. |
| `FLYWAY_ENABLED` | optional | `true` only for the service that migrates (admin). Default is correct. |

Business rules (payment window, delivery fee, free-delivery threshold, pickup lead time, points value and cap, return window, auto-confirm days,
experience cancellation notice) are not environment variables: staff change them in *Orders > Settings*, and they apply immediately.

Other tunables (all optional): `chocomall.login.max-attempts`, `chocomall.login.lockout-minutes`, `chocomall.jobs.enabled`,
`secure.jwt.expiration-seconds`, `chocomall.storage.public-base-url`.

## Order of operations for a release

1. Build images (`docker compose build`) and take a database backup.
2. Start the new **admin** first: it applies pending Flyway migrations.
3. Roll portal and search. Services are stateless, so several instances can run behind a load balancer; Redis holds the shared cache and codes, RabbitMQ delivers
   delayed messages to whichever instance consumes them, and the scheduled safety-net jobs are idempotent so running them on every instance is harmless.
4. Never edit an applied migration. Add `V3__...sql` (see `tools/model-generator/README.md`).

## Security checklist

* [ ] Every value in `.env` replaced; `.env` not committed.
* [ ] TLS terminated in front of the services (reverse proxy or ingress); the compose file publishes plain HTTP.
* [ ] The back-office API (8080) reachable only from the staff network or VPN.
* [ ] `/actuator/**` other than health/info stays authenticated; to scrape Prometheus metrics, expose `/actuator/prometheus` on a private network only.
* [ ] Swagger UI disabled or restricted in public deployments (`springdoc.swagger-ui.enabled=false`).
* [ ] MySQL, Redis, RabbitMQ, Elasticsearch and MinIO not published on public interfaces (the compose file publishes only the two consoles, on 127.0.0.1).
* [ ] The mock payment gateway replaced by a real provider (implement `PaymentGateway`, mark it `@Primary`).
* [ ] An SMS provider wired in (implement `SmsSender`); the default only logs, and the code is never returned outside `dev`.
* [ ] Backups for MySQL and the MinIO bucket; a restore rehearsal.
* [ ] Allergen data, shelf lives and dietary claims reviewed against real recipes and local rules before selling.

## Verification status (be aware)

| Area | Verified how |
|---|---|
| Business rules, security, API behaviour | 137 automated tests and an end-to-end smoke script against the running services |
| Schema and entity mapping | Flyway migrations run on H2 (MySQL mode) and Hibernate validates every entity against them |
| MySQL, Redis, RabbitMQ, MinIO, Elasticsearch | Implemented and compiled; the SQL is standard and portable; **not executed in the development environment** (no container runtime was available). Run `docker compose up --build` and `python tools/smoke_test.py` once as the acceptance step. The in-memory store, cache, messaging stub and database search follow the same interfaces and are fully tested. |

Things most worth checking on the first real MySQL run: the Flyway migrations (they use `AUTO_INCREMENT`, `DATETIME(6)` and `BOOLEAN`), the time zone
(the images set UTC), and the RabbitMQ queues being created on first connection.

## Troubleshooting

| Symptom | Likely cause |
|---|---|
| Portal or search fails with "table not found" | the admin has not run the migrations yet; start it first (or set `FLYWAY_ENABLED=true` for one service). |
| Start-up fails with `JWT_SECRET` errors | the variable is missing or not valid Base64 of at least 32 bytes. |
| `Database may be already in use` with H2 in dev | start the admin first; the other services attach to it (`AUTO_SERVER`). Delete `~/.chocomall/db` to reset the demo data. |
| 401 on every back-office call after a restart | tokens are signed with `JWT_SECRET`; a changed secret invalidates them. |
| 403 for a staff member | their role has no resource matching the path (*System > Roles > resources*). |
| Orders stay unpaid forever | `chocomall.jobs.enabled=false` and no broker; the cancellation job or RabbitMQ must be active. |
