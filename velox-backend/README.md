# VELOX Backend

Spring Boot 3.3 REST API + console app sharing one MySQL database (`velox_db`).

## Architecture note (read first)

This project uses **raw JDBC** (`java.sql` via `util/DatabaseConnection`), **not
JPA/Hibernate**. There are no `@Entity` classes: every query lives in `dao/*`
and DTOs in `dto/*` are populated manually from `ResultSet`s. Keep it that way
unless the team explicitly migrates to Spring Data JPA.

**Single source of truth is MySQL.** File stores under `data/` (`orders.dat`,
per-user folders, `transactions.log`) are local caches/audit trails for the
console app — all API reads/writes go to the database.

## Run

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-25.0.4.7-hotspot'
cd velox-backend
.\mvnw.cmd spring-boot:run
```

DB config: `src/main/resources/application.properties` (local only, **git-ignored** —
copy `application.properties.example`). Env vars win when set:
`VELOX_DB_URL`, `VELOX_DB_USER`, `VELOX_DB_PASSWORD`.
Requires MySQL with `velox_db` imported (see `velox-db/VELOX_D2.SQL` + `velox-db/migrations/` in numeric order).

Console app: `.\mvnw.cmd exec:java` (runs `com.app.main.Main`). Note: `java -jar`
runs the API (`VeloxApplication`), not the console.

## Endpoints (all JSON, `/api` prefix, CORS open)

| Method & path | Auth | Description |
|---|---|---|
| POST `/auth/register` | no | `{email,password,name\|full_name,phone\|phone_number,governorate}` → `{token,user}` |
| POST `/auth/login` | no | `{email,password}` → `{token,user}` |
| POST `/auth/logout` | Bearer | revokes token |
| GET `/auth/profile?userId=` | Bearer (self) | profile + loyalty + offers |
| PUT `/auth/profile` | Bearer (self) | update name/phone/governorate |
| GET `/products` | no | catalog in storefront shape (incl. `image`, Arabic names) |
| GET `/api/categories` … `/categories` | no | all/food/fashion/electronics with counts |
| GET `/governorates`, `/{name}`, `/{name}/shipping` | no | 27 governorates, codes, prices |
| GET `/orders/history?userId=&page=&size=` | Bearer (self) | paged history from MySQL |
| POST `/orders` | Bearer | `{items:[{product_id,quantity}], governorate?, paymentMethod?}` server-priced |
| GET `/orders/{id}/tracking` | Bearer (owner) | timeline + driver location |
| PATCH `/orders/{id}/status?status=` | Bearer (owner) | forward-only transitions, persisted |
| GET `/orders/{id}/invoice` | Bearer (owner) | `application/pdf` (Arabic-capable) |
| GET `/offers/personalized?userId=` | Bearer (self) | offers from purchase history |
| GET `/loyalty/status?userId=` | Bearer (self) | progress toward free delivery |
| GET `/wallet/balance?userId=` | Bearer (self) | `remaining_budget` |
| POST `/wallet/topup` | Bearer | `{amount}` |
| POST `/reviews` | Bearer | `{orderId?, rating 1-5, comment?}` |
| POST `/complaints` | Bearer | `{orderId?, details}` |

Error shape: `{ "message": "..." }` with 400/401/403/404/500 as appropriate.

## Background jobs

`DeliverySimulationService` (`@Scheduled`, 30s tick) advances PENDING (older
than 60s) → PROCESSING → IN_TRANSIT → ARRIVED → DELIVERED, writing
`orders.status`, `deliveries` and `driver_locations` (system courier
`courier@velox.local`).

## Tables with no writers (documented, not wired)

`payments`, `promo_codes`, `promotions_and_offers`, `reviews_and_complaints`:
schema exists for future use; nothing reads/writes them yet. Notifications are
frontend-local only (no `notifications` table by design decision).

## Migrations

Versioned SQL in `migrations/` (apply in order): `V2` (27 governorates,
`SHIPPED`, `order_code`, loyalty counter, `sessions`, product images/Arabic),
`V3` (deliveries zone widen, nullable review/complaint order links, courier).
