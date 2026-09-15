# VELOX — All-in-One Express Delivery

English technical documentation for the VELOX monorepo: Spring Boot REST API + console app + web storefront + MySQL, all running on **one database** as the single source of truth.

> Task coverage: **KAN-130** (documentation) and **KAN-126** (backend unit tests + Postman suite).
> Full endpoint map: [`velox-backend/README.md`](velox-backend/README.md) · Test suite: [`velox-backend/src/test`](velox-backend/src/test) · Postman: [`velox-backend/postman/VELOX_API_Tests.postman_collection.json`](velox-backend/postman/VELOX_API_Tests.postman_collection.json) · Printable technical doc: [`docs/VELOX_Technical_Documentation.pdf`](docs/VELOX_Technical_Documentation.pdf)

```
velox-project/
├── velox-backend/                 # Spring Boot 3.3 API + console app (Maven, Java 17)
│   ├── src/main/java/com/app/     # controller, service, dao, dto, enums, model, util, ...
│   ├── src/test/java/com/app/     # JUnit 5 + Mockito suite (35 tests, KAN-126)
│   ├── postman/                   # Postman E2E collection with assertions (KAN-126)
│   ├── velox-db/                  # VELOX_D2.SQL base dump + migrations/ V2..V6
│   ├── pom.xml
│   └── README.md                  # backend-only run + endpoint reference
├── velox-frontend/                # storefront (HTML/CSS/JS, no build step)
│   ├── index.html                 # entry page (start here)
│   ├── account/orders/...html     # account, orders, wallet, support pages
│   └── js/ + css/ + assets/       # services, i18n (AR/EN), theme
├── docs/                          # printable documentation
│   └── VELOX_Technical_Documentation.pdf
├── .gitignore
└── README.md                      # (this file)
```

## 1. System architecture

```mermaid
flowchart LR
    FE[Web storefront\nHTML/CSS/JS] -->|REST + Bearer token| API[Spring Boot API\n13 controllers]
    CLI[Console app\ncom.app.main.Main] --> DB[(MySQL velox_db)]
    API -->|raw JDBC| DB
    API -->|@Scheduled 30s| SIM[Delivery simulator]
    SIM --> DB
```

| Layer | Technology | Notes |
|---|---|---|
| API | Spring Boot 3.3, Java 17, embedded Tomcat :8080 | CORS open, token auth (`Authorization: Bearer`), no Spring Security auto-config (BCrypt only) |
| Persistence | Raw JDBC (`util/DatabaseConnection`), **no JPA** | Every query lives in `dao/*`; DTOs are filled manually from `ResultSet` |
| Auth | Email + password + 6-digit OTP, tokens in `sessions` table | Accounts stay inactive until OTP verified |
| DB | MySQL 8 (`velox_db`) | Base dump + V2..V6 migrations; MariaDB needs the `utf8mb4_0900_ai_ci` → `utf8mb4_unicode_ci` swap |
| Frontend | Static HTML/CSS/JS, `API_BASE_URL=http://localhost:8080/api` | Arabic/English i18n, RTL/LTR |
| Background | `DeliverySimulationService` tick 30s | PENDING (>60s) → PROCESSING → IN_TRANSIT → ARRIVED → DELIVERED |

Order tracking state machine: `PENDING → PROCESSING → SHIPPED → DELIVERED` (API level; `SHIPPED` is stored/shown as `IN_TRANSIT`, transitions are forward-only).

## 2. API reference (all JSON, `/api` prefix)

Auth: `Bearer <token>` from `POST /auth/login` (or `/verify-otp`). Error shape: `{ "message": "..." }` with 400/401/403/404/500.

| Method & path | Auth | Description |
|---|---|---|
| POST `/auth/register` | no | `{email,password,full_name,phone_number,governorate}` → `{pending:true, otp}` (dev) |
| POST `/auth/verify-otp` | no | `{email, code}` → `{token, user}` |
| POST `/auth/resend-otp` | no | re-issues OTP for unverified accounts |
| POST `/auth/login` | no | `{email,password}` → `{token,user}` (401 if unverified) |
| POST `/auth/logout` | Bearer | revokes token |
| GET `/auth/profile?userId=` | Bearer (self) | profile + loyalty + offers |
| PUT `/auth/profile` | Bearer (self) | update name/phone/governorate |
| GET `/products` | no | catalog with images + Arabic names |
| GET `/categories` | no | all/food/fashion/electronics with counts |
| GET `/governorates`, `/{name}`, `/{name}/shipping` | no | 27 governorates, codes 1–27, prices (Cairo 20 EGP … New Valley 80) |
| GET `/orders/history?userId=&page=&size=` | Bearer (self) | paged history |
| POST `/orders` | Bearer | `{items:[{product_id,quantity}], governorate?, paymentMethod?}` server-priced |
| GET `/orders/{id}/tracking` | Bearer (owner) | timeline + driver location |
| PATCH `/orders/{id}/status?status=` | Bearer (owner) | forward-only, persisted |
| GET `/orders/{id}/invoice` | Bearer (owner) | `application/pdf` (Arabic-capable) |
| GET `/offers/personalized?userId=` | Bearer (self) | offers from purchase history |
| GET `/loyalty/status?userId=` | Bearer (self) | progress toward free delivery |
| GET `/wallet/balance?userId=` | Bearer (self) | `remaining_budget` |
| POST `/wallet/topup` | Bearer | `{amount}` |
| POST `/reviews` | Bearer | `{orderId?, rating 1-5, comment?}` |
| POST `/complaints` | Bearer | `{orderId?, details}` |

## 3. Database schema map (`velox_db`)

| Table | Key columns |
|---|---|
| `users` | id, full_name, email, password_hash, phone_number, governorate, is_verified, is_active, current_budget, remaining_budget, role, loyalty_rewards_consumed |
| `orders` | id, order_code, user_id, status, total_amount, discount_amount, delivery_fee, final_amount, shipping_address, is_returned, order_date |
| `order_items` | id, order_id, product_id, quantity, unit_price, subtotal |
| `products` | id, store_id, category_id, name, name_ar, description(_ar), image_url, price, stock_quantity, product_type, size, is_available |
| `sessions` | token, email, expires_at |
| `otp_codes` | id, email, code_hash, expires_at, attempts, used |
| `deliveries` | id, order_id, driver_id, zone, delivery_status, delivery_fee, assigned_at, delivered_at |
| `driver_locations` | driver_id, latitude, longitude, updated_at |
| `invoices` | invoice_id, order_id, user_id, invoice_pdf_path |
| `return_requests` | id, order_id, user_id, status |
| `user_notifications` | id, user_id, title, message, type, is_read |
| `user_payment_cards` | id, user_id, method_type, cardholder_name, last_4_digits, card_brand, provider |

Seed: `products ≈ 25` rows. Migrations V2 (governorates, SHIPPED, sessions, images), V3 (deliveries widen, nullable review/complaint links), V4 (returns, saved cards), V5 (notifications), V6 (payment methods, OTP).

## 4. Run from scratch (any machine)

Requirements: **JDK 17+**, **MySQL 8** running, a browser, internet on first Maven run.

### 4.1 Database (once)
1. Create empty database `velox_db`.
2. Import the base dump `VELOX_D2.SQL` first (**not tracked in Git by design** — `*.SQL` is git-ignored because dumps may carry local data; get the file from the repo maintainer or your teammate's `velox-backend/velox-db/` copy).
3. Apply `velox-backend/velox-db/migrations/` in numeric order (V2 → V6).
4. Verify: `products ≈ 25` rows; tables `sessions`, `return_requests`, `user_notifications`, `otp_codes` exist.

### 4.2 Backend config (once per machine)
Copy `velox-backend/src/main/resources/application.properties.example` → `application.properties` (same folder, git-ignored) and set your MySQL password — or preferably env vars (they always win):
```powershell
setx VELOX_DB_URL "jdbc:mysql://localhost:3306/velox_db"
setx VELOX_DB_USER "root"
setx VELOX_DB_PASSWORD "your-machine-password"
```

### 4.3 Backend
```powershell
cd velox-backend
# with the Maven wrapper if present, otherwise any Maven 3.9+:
mvn spring-boot:run
```
Wait for `Started VeloxApplication`, then check `http://localhost:8080/api/governorates/CAIRO` (must return JSON).

### 4.4 Frontend
Double-click `velox-frontend/index.html` (backend must run first). Register → confirm OTP → browse → top up wallet → order → download invoice → track.

### 4.5 Console (optional)
```powershell
cd velox-backend
mvn exec:java
```
Note: `java -jar` runs the API, not the console.

## 5. QA — tests & validation (KAN-126)

Unit suite: **35 tests, all green** (`mvn test` in `velox-backend`):

| Test class | Tests | What it covers |
|---|---|---|
| `enums.GovernorateTest` | 9 | 27 governorates, codes 1–27, core prices, `fromName/Code/Lenient/CodeOrName`, rejections |
| `controller.GovernorateControllerTest` | 5 | MockMvc: list size 27, case-insensitive lookup, 404s, shipping payload |
| `controller.OrderControllerTest` | 9 | Mockito-mocked service: 200/400/401/403/404 paths for history, tracking, status update |
| `service.OrderServiceTest` | 8 | Mockito static-mocked JDBC: paging, DTO mapping, illegal-transition guard, ownership |
| `dto.OrderDtoTest` | 3 | `OrderHistoryDto.from`, paged envelope, tracking timeline flags |
| `service.ReturnServiceTest` | 1 | legacy JUnit 4 (runs via vintage engine) |

E2E: import `velox-backend/postman/VELOX_API_Tests.postman_collection.json` into Postman and run folder-by-folder (Auth → Governorates → Orders → Catalog). Every request ships `pm.test` success **and** failure assertions (401/403/404/400). All scenarios were executed against a live server during development.

## 6. Growth features (competitive edge over Talabat)
- **Unified multi-store cart** — one checkout across stores, server-priced (`POST /api/orders/quote`, +10 EGP per extra store, per-store breakdown in the response and cart drawer).
- **Instant wallet refund** — one-tap compensation for delivered orders (`POST /api/wallet/refund`, LATE/WRONG_ITEM/DAMAGED rules, once per order).
- **Scheduled orders** — `scheduled_for` on checkout; the simulator only picks up due orders.
- **VELOX Plus** — 50 EGP / 30 days, free delivery (`GET|POST /api/loyalty/subscription|subscribe`, panel on the account page).
- **Price watches** — bell on product cards (`/api/watches` CRUD, hourly sweep notifies on drops).

## 7. Notes for developers
- Raw JDBC by decision — any query goes in `velox-backend/src/main/java/com/app/dao/`.
- MySQL is the single source of truth; `data/` files are local caches (ignored).
- Work on `main`. Never push secrets, `application.properties`, or `*.dat`.
- Common issues: `Access denied` = MySQL down or wrong password; `Port 8080 in use` = old server window still open; red in IntelliJ = Maven reload + JDK 17+ + annotation processing (Lombok); `Unknown collation utf8mb4_0900_ai_ci` = you are on MariaDB, swap to `utf8mb4_unicode_ci` for local import only.
