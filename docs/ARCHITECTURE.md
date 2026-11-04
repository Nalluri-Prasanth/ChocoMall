# Architecture and design decisions

## 1. Overview

ChocoMall is a modular monolith split into three deployable Spring Boot services that share one database and a set of libraries.
The services are split (admin, portal, search, plus shared modules) because the three audiences scale, deploy and fail independently: staff use the back office, customers hit the storefront, and search is read-heavy.

```
choco-common   <-  choco-model  <-  choco-core  <-  choco-admin
                        ^              ^        <-  choco-portal
                        |              +--------<-  choco-search
choco-security  <-------+-------------------------  choco-admin, choco-portal
```

* **choco-common** (no business knowledge): `CommonResult`/`CommonPage` envelope, `ApiException` and `Asserts`, the global exception
  handler, paging helper, serial-number generator, request-id filter.
* **choco-model**: 51 entities, 23 enums, Spring Data repositories, Flyway migrations and the seed loaders. No business logic.
* **choco-security**: JWT service, authentication filter, resource-pattern authorization, 401/403 writers, CORS.
* **choco-core**: the rules that more than one application needs: `StockService`, `OrderLifecycleService` (state machine),
  `PaymentService`, `LoyaltyService`, `PersonalizationService`, `ExperienceBookingService`, messaging, file storage, caches, jobs.
* **choco-admin / choco-portal / choco-search**: controllers, application services and DTOs for each audience.

A rule lives in `choco-core` when both the back office and the storefront must apply it identically (for example, cancelling an order
must release stock whether a customer or a staff member does it). Anything specific to one audience stays in that application.

## 2. A request, end to end

```
HTTP -> RequestIdFilter (X-Request-Id, logging context)
     -> CORS
     -> JwtAuthenticationFilter  (bearer token -> user -> SecurityContext)
     -> authorization            (admin: URL pattern granted by a role;  portal: signed-in member)
     -> Controller               (validation with Jakarta annotations)
     -> Service                  (@Transactional business rules)
     -> Repository               (Spring Data JPA, atomic UPDATEs for counters)
     <- CommonResult {code, message, data}   (errors via GlobalExceptionHandler / SecurityExceptionAdvice)
```

Every failure has the same shape and an HTTP status that matches `code`: 400 validation, 401 not signed in, 402 payment declined,
403 not allowed, 404 not found, 409 conflicts with the current state (for example "not enough stock", "order already paid"),
429 too many attempts, 500 unexpected (details are logged, never returned).

## 3. Security

| Concern | Decision |
|---|---|
| Authentication | Stateless JWT (HS256). The key is Base64 from configuration; the `prod` profile has **no default** so a missing `JWT_SECRET` stops start-up. |
| Two audiences | Staff and customer tokens carry a different `aud` claim and each service only accepts its own. A customer token cannot be replayed against the back office even if usernames collide (tested). |
| Back-office authorization | **Roles → resources → URL patterns.** A staff member's authorities are the patterns of the resources granted to their *active* roles (for example `/product/**`). A request is allowed only if a pattern matches; unknown paths are denied, so a new endpoint is closed until a resource grants it. The loaded permissions are cached and evicted whenever roles, resources or accounts change, so disabling a role takes effect immediately (tested). |
| Super-admin safety | The `SUPER_ADMIN` role cannot be edited, disabled or deleted; the last super admin cannot be removed, demoted or disabled; nobody can delete or disable their own account. New resources are attached to super admins automatically. |
| Passwords | BCrypt. Never returned by any endpoint (tested). |
| Brute force | Sign-in is locked for 15 minutes after 5 failures per username (configurable). Verification codes are single-use, expire after 5 minutes, allow 5 attempts and one request per minute per phone number. |
| Customer data | Flavor preferences are stored only after explicit consent, can be deleted, and nothing else is profiled. Orders and personalisation requests are visible only to their owner (a foreign id gives 404, not 403, so ids cannot be probed). |
| Claims | Dietary and sourcing claims ("vegan", "halal", "single origin"...) are hidden from shoppers until staff record evidence and verify them. |
| Files | Uploads accept PNG/JPEG/WebP/GIF up to 5 MB; the extension is derived from the content type, never from the client file name. |
| Web | CSRF protection is off because the API is stateless and uses no cookies; CORS allows only configured origins. |

## 4. Persistence

* **Flyway** owns the schema (`V1` tables, `V2` baseline permissions). The same SQL runs on H2 (MySQL mode) in development and tests and on MySQL 8 in production.
* **Single source of truth.** Tables, entities, repositories and enums are generated from `tools/model-generator/schema.py`, so SQL and
  Java cannot drift apart. Tests boot with `ddl-auto=validate`, which proves the entities match the migrations.
* **Entities hold ids, not object graphs.** Relations are plain `Long` columns plus foreign keys in the database; collections of ids
  (`flavorIds`, `roleIds`...) are element collections. This avoids lazy-loading surprises and makes serialisation safe.
* **Counters are never saved from entities.** Stock, reserved stock, units sold, reserved preorder capacity, booked seats, loyalty
  points and coupon counts are `updatable=false` and are only changed by conditional UPDATE statements
  (`... where stock - lock_stock >= :units`). That makes overselling impossible under concurrency without locks, and it means a stale
  edit form can never overwrite stock.
* **Optimistic locking** (`@Version`) protects orders, products, preorder windows and personalisation requests.
* **Soft delete** for products, so past orders keep their history; orders are hidden, never removed.
* **Money** is `BigDecimal` with two decimals; discounts are allocated to order lines so refunds can be computed exactly.

## 5. Consistency rules worth knowing

**Stock.** Placing an order *reserves* units (`lockStock`); payment turns the reservation into a sale; cancelling releases it (or restocks
if it was already paid). Every movement is written to `pms_stock_movement` with its reason and reference. Only items that sit on a
shelf are tracked: made-to-order drinks, experiences, personalised items, preorders and build-a-box products draw down other things
(preorder window capacity, or the individual bonbons chosen for the box).

**Order state machine** (`OrderStateMachine`):

```
PENDING_PAYMENT -> PREPARING -> READY_FOR_PICKUP -> COMPLETED     (pickup orders)
                             -> SHIPPED          -> COMPLETED     (delivery orders)
PENDING_PAYMENT / PREPARING -> CANCELLED          PENDING_PAYMENT / CANCELLED -> INVALID
```

All transitions go through `OrderLifecycleService`, which moves stock, preorder capacity, coupons, points and the audit trail together.
It is idempotent where retries happen: a repeated payment callback does not double-count points or stock, cancelling twice is harmless, and a
payment that arrives after the timeout is refused.

**Checkout** is one transaction: price from the SKU (never from the client) → reserve preorder capacity → lock stock in a fixed product
order (no deadlocks) → redeem points → mark the coupon used → write the order and lines → clear the basket lines → schedule the
payment-window message after commit.

**Payment window.** Unpaid orders (and unpaid experience bookings) are cancelled automatically. RabbitMQ delivers a delayed message (TTL queue
with a dead-letter exchange); a scheduled job repeats the check every minute, so the system is correct
even if the broker is down or a message is lost.

**Loyalty.** Points = floor(paid amount × points-per-currency × level multiplier). Redemption is capped to a share of the order. Every earn,
redeem and reversal is recorded with the order it came from; refunds reverse earned points without letting the balance go negative.

## 6. Infrastructure switches

The same code runs with or without external services; the profile chooses.

| Concern | Development (`dev`) | Production (`prod`) | Property |
|---|---|---|---|
| Database | H2 file `~/.chocomall`, MySQL mode | MySQL 8 | `spring.datasource.*` |
| Cache | Caffeine | Redis | `spring.cache.type` |
| Codes and throttling | in memory | Redis | `chocomall.store.type` |
| Messaging | none (job-based) | RabbitMQ | `chocomall.messaging.type` |
| Files | local disk `/files/**` | MinIO | `chocomall.storage.type` |
| Search | shop database | Elasticsearch | `search.engine` |
| Demo data | loaded | not loaded | `chocomall.seed.*` |

## 7. Search

`SearchEngine` has two implementations selected by `search.engine`. The **database engine** queries the live tables with the same
filters and is always current. The **Elasticsearch engine** indexes published products (name, tags, flavors, category and brand are weighted;
typos are tolerated), rebuilds with `POST /search/index/importAll`, and is kept current from `changed:`/`removed:` events that the back office
publishes to RabbitMQ after each product transaction commits. Index management requires the `X-Search-Admin-Key` header.

## 8. Decisions and trade-offs

| Decision | Why | Cost |
|---|---|---|
| Spring Data JPA instead of MyBatis (as in `mall`) | Less boilerplate, derived queries, specifications for dynamic filters, schema validation | Less control over hand-written SQL |
| One shared database | Matches the reference design; consistent transactions across stock, orders and loyalty | Services are coupled through the schema; the admin must migrate first |
| `choco-core` shared library | One implementation of each business rule | A change to core redeploys all three services |
| Demo prices computed from a category table | The guide contains no prices on purpose | Must be replaced before real use |
| Mock payment gateway behind an interface | Lets the full flow run anywhere; a real provider is one class | No real money movement out of the box |
| Launch tiers drive visibility | Reflects the guide's "start small, expand after demand": Core and Expand are live, Specialist stays in draft | Staff publish new items deliberately |
| Per-process cache in the storefront (home page, 5 minutes) | Cheap and simple | Home page edits appear after the cache expires; a shared Redis cache plus an eviction message would remove the delay |

## 9. Known limitations

* Tokens cannot be revoked before they expire (a deny list or short-lived tokens with refresh would fix this).
* No subscription boxes (guide item S16) and no ingredient-lot tracking (M02), by design for the first release.
* Delivery has no courier integration or time slots; the back office records the courier and tracking number by hand.
* Reports are computed on demand from orders; very large shops would want pre-aggregated tables.
