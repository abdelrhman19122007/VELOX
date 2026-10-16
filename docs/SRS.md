# Software Requirements Specification (SRS)
## VELOX — Local delivery platform for Samannoud

| | |
|---|---|
| Version | 2.0 (rebuild) |
| Date | October 2026 |
| Programme | Digital Egypt Pioneers Initiative (DEPI) — Penetration Testing track |
| Team | 5 members (see §9) |
| Repository | https://github.com/abdelrhman19122007/VELOX |
| Standard | Structure based on IEEE 830 / ISO/IEC/IEEE 29148 |

---

## 1. Introduction

### 1.1 Purpose
This document specifies the functional and non-functional requirements of VELOX version 2: a web and mobile platform
that lets people in Samannoud (Gharbia, Egypt) and the surrounding villages order from local restaurants, shops and
pharmacies and get them delivered by local drivers. It is the reference for design, implementation, testing
(including security testing) and acceptance.

### 1.2 Scope
VELOX consists of:
- **Backend** — a Java 17 / Spring Boot REST API with a PostgreSQL database (the single source of truth).
- **Web application** — for customers, stores, drivers and the administrator.
- **Mobile application** — Flutter (Android first; published on Google Play), one app whose screens depend on the
  signed-in role.

In scope: accounts, catalogue, ordering, delivery zones, live order tracking, payments (cash, Vodafone Cash, Fawry,
in-app wallet), notifications, store / driver / admin dashboards, reviews and complaints, security controls.
Out of scope for v2: card payments, iOS App Store release, multi-city operation outside Samannoud's area.

### 1.3 Definitions
| Term | Meaning |
|---|---|
| Customer | person who orders |
| Store | restaurant, grocery, pharmacy or shop selling through VELOX (also "restaurant account") |
| Driver | person who delivers (motorbike, tuk-tuk, bicycle) |
| Admin | platform operator |
| Zone | delivery area (a part of Samannoud or a village) with a fixed fee |
| Errand | "bring me anything": the driver buys from any shop on the customer's behalf |
| COD | cash on delivery |
| Live update | change pushed to open apps without refreshing (SSE / long polling) |
| OWASP Top 10 | the ten most critical web application security risks (2021 edition) |

### 1.4 References
- IEEE Std 830-1998, ISO/IEC/IEEE 29148:2018
- OWASP Top 10 (2021), OWASP ASVS 4.0 level 2, OWASP Mobile Top 10 (2024)
- Google Play Developer Policy; Egyptian Personal Data Protection Law No. 151 of 2020
- `docs/database/ERD.md` — database design

### 1.5 Problem statement
Big delivery apps cover Samannoud poorly and list few local shops. Orders are taken by phone or WhatsApp with no
menu, no fixed prices and no tracking; addresses are described by landmarks and often misunderstood. Small shops
have no simple way to sell online, while many young people with motorbikes or tuk-tuks look for work.

---

## 2. Overall description

### 2.1 Product perspective
A three-tier system: clients (web, mobile) → REST API (stateless, token authenticated) → PostgreSQL.
Live updates use Server-Sent Events with a long-polling fallback. Push notifications use Firebase Cloud Messaging.
Maps use OpenStreetMap. Payments go through a gateway interface (sandbox until a merchant account is connected).

### 2.2 Product functions (summary)
1. Sign up / sign in, roles, password change and reset, saved addresses with landmarks.
2. Browse and search stores and products; favourites.
3. Cart (one store per order), errands, pharmacy orders with a prescription photo.
4. Pricing: zone delivery fee, packaging, promo codes, loyalty points.
5. Payment: cash (with change amount), Vodafone Cash, Fawry, wallet; refunds.
6. Order lifecycle with live tracking on a map.
7. Store dashboard, driver app, admin panel.
8. Reviews, complaints with compensation, notifications.

### 2.3 User classes
| Class | Description | Technical skill |
|---|---|---|
| Customer | residents of Samannoud and its villages, all ages | low to medium; Arabic first |
| Store | owners / staff of local shops | low; needs a very simple dashboard |
| Driver | local young people with a vehicle | low; uses a phone on the move |
| Admin | VELOX operators | medium |
| Phone-only customer | elderly people without a smartphone | none — the admin places the order for them |

### 2.4 Operating environment
- Backend: Java 17+, Spring Boot 3, PostgreSQL 16, any Docker host (512 MB RAM is enough).
- Web: current Chrome, Edge, Firefox, Safari; phone and desktop screens.
- Mobile: Android 7.0 (API 24) and later.
- Network: must work on slow mobile data (village coverage).

### 2.5 Design and implementation constraints
- **Language: Java** for all server-side code (programme requirement); Flutter for the mobile app.
- Clean architecture (controller → service → repository → domain), SOLID, no business logic in controllers.
- Database changes only through versioned migrations.
- Every module owner commits their own work through pull requests (programme requirement).
- No secrets in the repository (passwords, keys, tokens come from environment variables).

### 2.6 Assumptions and dependencies
- Stores keep their menu and availability up to date.
- Real online payments require a merchant account (Fawry / Vodafone Cash); until then the payment gateway runs in
  sandbox mode and says so in the UI.
- Google Play publishing requires a developer account and, for new personal accounts, a 14-day closed test with
  at least 12 testers before production release.

---

## 3. Functional requirements

Priority: **M** = must, **S** = should, **C** = could. Each requirement is traced to a module owner and tests in §8.

### 3.1 Accounts & security — module AUTH (Nabeeh (@nabosallem-svg))
| ID | Requirement | Pri |
|---|---|---|
| FR-AUTH-01 | A visitor can register as a customer or driver with name, email, Egyptian mobile number (01[0125]xxxxxxxx) and a password of at least 8 characters. Store and admin accounts are created only by the admin. | M |
| FR-AUTH-02 | A user can sign in with email and password and receives a signed session token valid for 30 days. | M |
| FR-AUTH-03 | After 5 wrong passwords for one email within 10 minutes, sign-in for that email is refused for 10 minutes; after 30 from one network, that network is refused. | M |
| FR-AUTH-04 | A user can change their password by giving the current one. | M |
| FR-AUTH-05 | "Forgot password": the admin issues a one-time temporary password; the user is asked to change it. | M |
| FR-AUTH-06 | A user can sign out of every device (all existing tokens stop working). | S |
| FR-AUTH-07 | A customer can save up to 10 addresses (label, zone, address, landmark, phone, map point) and pick one at checkout. | M |
| FR-AUTH-08 | Blocked accounts cannot sign in and their sessions stop working immediately. | M |
| FR-AUTH-09 | Sensitive actions (block, password reset, refund, admin cancel) are written to an audit log with actor, time and IP. | M |
| FR-AUTH-10 | Users can switch language (Arabic / English) and theme (light / dark). | S |

### 3.2 Catalogue — module CATALOG (Hoda (@hodayasser511-stack))
| ID | Requirement | Pri |
|---|---|---|
| FR-CAT-01 | Anyone can list stores with photo, category, rating, opening hours and open/closed state. | M |
| FR-CAT-02 | Anyone can view a store's menu grouped by section, with prices and availability. | M |
| FR-CAT-03 | Search by store name, category or product name (Arabic and English). | M |
| FR-CAT-04 | Filter (category, open now, favourites) and sort (recommended, cheapest, rating). | S |
| FR-CAT-05 | A store is orderable only within its opening hours and while its "accepting orders" switch is on. | M |
| FR-CAT-06 | A store account can mark products unavailable and change prices. | M |
| FR-CAT-07 | The admin can add stores (with their sign-in) and products, edit them, and upload photos (JPEG/PNG/WebP, ≤ 2 MB). | M |
| FR-CAT-08 | Customers can mark stores and products as favourites. | C |

### 3.3 Ordering & delivery — module ORDER (Abdelrhman (@abdelrhman19122007))
| ID | Requirement | Pri |
|---|---|---|
| FR-ORD-01 | The cart holds products of one store; adding from another store asks to start a new cart. | M |
| FR-ORD-02 | Each cart line can carry a note ("no onions"). | S |
| FR-ORD-03 | Prices, fees and discounts are always computed by the server; client-sent prices are ignored. | M |
| FR-ORD-04 | Delivery fee = the fixed fee of the chosen zone (Samannoud areas / villages), managed by the admin. | M |
| FR-ORD-05 | At checkout the customer gives zone, address, landmark, phone and optionally a map point. | M |
| FR-ORD-06 | Errand orders: the customer describes or photographs what to buy from any shop; the driver buys and delivers. | S |
| FR-ORD-07 | Pharmacy orders can carry a prescription photo. | S |
| FR-ORD-08 | Orders can be scheduled for a later time (e.g. iftar). | C |
| FR-ORD-09 | The order follows the lifecycle in ERD.md; only the allowed actor can make each move (customer cancels only before acceptance, store accepts/rejects/ready, driver picks up/delivers). | M |
| FR-ORD-10 | Orders the store doesn't answer in 15 min, or no driver takes in 40 min, are cancelled automatically (and refunded if paid). | M |
| FR-ORD-11 | The customer sees the order status, timeline and the driver's position on a map, updated live. | M |
| FR-ORD-12 | A delivered order can be returned within 1 day (product value refunded, delivery not). | S |
| FR-ORD-13 | A customer can re-order a past order in one tap. | C |

### 3.4 Payments, wallet & notifications — module PAY (Fady (@fadyza15-collab))
| ID | Requirement | Pri |
|---|---|---|
| FR-PAY-01 | Cash on delivery; the customer may enter the note they will pay with so the driver brings change. | M |
| FR-PAY-02 | Vodafone Cash: the customer enters a 010 wallet number and confirms with the code sent to it (10 min, 5 tries). | M |
| FR-PAY-03 | Fawry: the customer receives a reference number to pay at an outlet or in myFawry (60 min). | M |
| FR-PAY-04 | An online order is hidden from the store until it is paid; unpaid orders expire automatically. | M |
| FR-PAY-05 | A paid order that is rejected, cancelled or returned is refunded and the refund is recorded. | M |
| FR-PAY-06 | Every payment attempt and refund is stored; a payment reference can never be used twice. | M |
| FR-PAY-07 | In-app wallet: balance from top-ups, refunds and compensation; can pay for orders. | S |
| FR-PAY-08 | Promo codes: percent, fixed amount or free delivery, with minimum order, expiry and use limits. | S |
| FR-PAY-09 | Loyalty points per delivered order, redeemable as a discount. | C |
| FR-PAY-10 | Push notifications (even with the app closed): store — new paid order; customer — every status change; driver — new delivery nearby. | M |
| FR-PAY-11 | An in-app notification inbox keeps the last notifications. | S |

### 3.5 Operations — module OPS (Raheek (@Raheek25))
| ID | Requirement | Pri |
|---|---|---|
| FR-OPS-01 | Store dashboard: incoming orders live with a sound alert; accept / reject with reason / mark ready; today's orders, sales and rating. | M |
| FR-OPS-02 | Store "accepting orders" switch. | M |
| FR-OPS-03 | Driver app: go online/offline, see ready orders sorted by distance, take one (one active delivery at a time), picked-up, delivered. | M |
| FR-OPS-04 | While online, the driver's location is sent every ≤ 5 s and shown to the customer and store. | M |
| FR-OPS-05 | Drivers see what to collect (cash orders) or "paid online — collect nothing", and their earnings. | M |
| FR-OPS-06 | Admin panel: today's numbers, all orders (cancel with reason), stores & products, zones & fees, complaints, accounts (block, reset password). | M |
| FR-OPS-07 | Admin can place an order on behalf of a phone-only customer. | S |
| FR-OPS-08 | Customers rate the store and the driver after delivery (1–5 + comment). | M |
| FR-OPS-09 | Customers can complain about an order; they get a compensation code at once and the admin follows up and closes it with a note. | M |

---

## 4. Non-functional requirements

### 4.1 Security (primary focus — Penetration Testing track)
| ID | Requirement | OWASP 2021 |
|---|---|---|
| NFR-SEC-01 | Every endpoint except catalogue, sign-in and sign-up requires a valid token; every resource checks ownership/role on the server (no IDOR: a user can never read or change another user's order, address or payment). | A01 Broken Access Control |
| NFR-SEC-02 | Passwords hashed with PBKDF2-HMAC-SHA256 (≥ 210 000 iterations, random salt); payment codes hashed; tokens HMAC-SHA256 signed with a secret from the environment. | A02 Cryptographic Failures |
| NFR-SEC-03 | All SQL uses parameterised queries; no string-built SQL with user input. All input validated (type, length, format, range) on the server. | A03 Injection |
| NFR-SEC-04 | Output is encoded in the web UI (no HTML injection); `Content-Security-Policy`, `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy` headers set. | A03 / A05 |
| NFR-SEC-05 | Business rules enforced on the server: prices, totals, status transitions, payment state, one active delivery per driver. | A04 Insecure Design |
| NFR-SEC-06 | No default credentials in production; demo accounts only with an environment-provided password; detailed errors never shown to clients; CORS limited to known origins. | A05 Security Misconfiguration |
| NFR-SEC-07 | Dependencies scanned in CI (OWASP Dependency-Check); no known critical CVE at release. | A06 Vulnerable Components |
| NFR-SEC-08 | Brute-force protection (FR-AUTH-03), session revocation (FR-AUTH-06, -08). | A07 Identification & Authentication Failures |
| NFR-SEC-09 | Uploaded files: type checked by content (magic bytes), size-limited, renamed, never executed, served from a separate path. | A08 Software & Data Integrity |
| NFR-SEC-10 | Security events (failed sign-ins, blocks, refunds, admin actions) logged with time and IP; no secrets or passwords in logs. | A09 Logging & Monitoring Failures |
| NFR-SEC-11 | Server-side requests only to fixed, allow-listed hosts (Firebase, OpenStreetMap tiles are client-side). | A10 SSRF |
| NFR-SEC-12 | HTTPS only in production (HSTS). Mobile app uses HTTPS only and stores the token in app-private storage. | Mobile M5 / M9 |
| NFR-SEC-13 | Personal data (phone, address) visible only to the order's customer, store, driver and admin (Law 151/2020). | — |

### 4.2 Performance
| ID | Requirement |
|---|---|
| NFR-PERF-01 | 95 % of API calls answer in < 500 ms with 100 concurrent users on a 1-vCPU / 512 MB server. |
| NFR-PERF-02 | Live updates reach other apps in < 3 s (SSE) or < 5 s (long-polling fallback). |
| NFR-PERF-03 | The mobile app opens to the home screen in < 3 s on a mid-range phone; images are compressed. |

### 4.3 Reliability & availability
- NFR-REL-01 Target availability 99 % during working hours (09:00–02:00).
- NFR-REL-02 No order or payment is lost if a client disconnects: every state change is in the database first.
- NFR-REL-03 Daily database backup, kept 7 days.
- NFR-REL-04 Clients reconnect live updates automatically and find the server again if its address changes.

### 4.4 Usability
- NFR-USE-01 Arabic (RTL) by default, English available; large touch targets; works on 5-inch screens.
- NFR-USE-02 Ordering in ≤ 5 taps from the home screen for a returning customer.
- NFR-USE-03 Clear error messages in the user's language (never stack traces).

### 4.5 Maintainability (clean code)
- NFR-MNT-01 Layered packages per module; controllers contain no business logic.
- NFR-MNT-02 Design patterns where they fit: Strategy (discounts, delivery fees, payment methods), State (order lifecycle), Factory (stores/products), Observer (events → live updates / push).
- NFR-MNT-03 Checkstyle (Google style, adapted) passes in CI; methods ≤ 40 lines where practical.
- NFR-MNT-04 Unit + integration test coverage ≥ 70 % of service code (JaCoCo).

### 4.6 Portability
- Backend runs in Docker anywhere (≤ 256 MB RAM measured); configuration only through environment variables.

---

## 5. External interfaces

### 5.1 User interfaces
Web and mobile share one design system (provided by the team): teal primary, orange accent, Arabic font Cairo,
light/dark themes. Main screens: welcome/sign-in, home, store, cart, checkout (map), payment, tracking, orders,
account; store dashboard; driver app; admin panel.

### 5.2 API
REST over HTTPS, JSON, prefix `/api`, documented with OpenAPI (Swagger UI at `/swagger-ui.html`) and a Postman
collection in `tests/postman`. Errors: `{ "code": "…", "message": "…", "details": {…} }` with proper HTTP status.

### 5.3 Communication
- Live updates: `GET /api/events` (SSE) and `GET /api/events/poll` (long polling).
- Push: Firebase Cloud Messaging HTTP v1.
- Maps: OpenStreetMap tiles (no API key).
- Payments: `PaymentGateway` interface — sandbox implementation now, Fawry / Vodafone Cash adapters later.

### 5.4 Hardware
Customer/driver phones with GPS (driver location, optional customer pin) and camera (prescription / list photos).

---

## 6. Data requirements
See `docs/database/ERD.md` for the full schema, constraints and data dictionary. Key rules: receipts are immutable
(item names and prices copied into the order), money in `NUMERIC(12,2)`, all timestamps with time zone (Africa/Cairo
for business rules), personal data deleted with the account (`ON DELETE CASCADE`) except orders kept for accounting.

---

## 7. Use cases (main)

| ID | Actor | Use case | Main flow |
|---|---|---|---|
| UC-01 | Customer | Place an order | browse → add to cart → checkout (zone, address, landmark) → choose payment → order placed → store notified |
| UC-02 | Customer | Pay with Vodafone Cash | choose VF Cash → enter wallet → receive code → enter code → order goes to the store |
| UC-03 | Customer | Track an order | open order → see steps + driver on the map live → rate after delivery |
| UC-04 | Store | Handle an order | alert → accept / reject → prepare → mark ready |
| UC-05 | Driver | Deliver | go online → take nearest order → pick up → deliver → collect cash if COD |
| UC-06 | Customer | Errand | write / photograph the list → choose zone → driver buys and delivers → pay the receipt + fee |
| UC-07 | Admin | Add a store | fill store data and hours → create the store's sign-in → add products |
| UC-08 | Admin | Handle a complaint | open complaint → call customer → close with a note |
| UC-09 | Admin | Phone order | create the order for a caller → it follows the normal lifecycle |
| UC-10 | Attacker (misuse case) | Read someone else's order | request another order id → server refuses (403/404) — verified in the pentest |

---

## 8. Verification — testing strategy and traceability

| Level | Tool | Scope | Owner |
|---|---|---|---|
| Unit | JUnit 5, Mockito | services, pricing, state transitions | every member, own module |
| Integration | Spring Boot Test + PostgreSQL (Testcontainers/H2) | API + database | every member |
| API | Postman/Newman, RestAssured | every endpoint, happy + error paths | Abdelrhman (@abdelrhman19122007) |
| UI / E2E | Selenium WebDriver | main web flows UC-01…UC-05 | Hoda (@hodayasser511-stack) |
| Mobile | Flutter widget & integration tests | sign-in, cart, checkout | Raheek (@Raheek25) |
| Performance | Apache JMeter | NFR-PERF-01/02 | Fady (@fadyza15-collab) |
| Security — static | SpotBugs + FindSecBugs, OWASP Dependency-Check | CI on every push | Nabeeh (@nabosallem-svg) |
| Security — dynamic | OWASP ZAP (baseline + active scan), Burp Suite | full API and web | all (own module) |
| Security — manual pentest | OWASP WSTG checklist | IDOR, auth, injection, business logic, file upload | all (own module) |
| Acceptance (UAT) | test-case table | every M requirement | Nabeeh (@nabosallem-svg) |

### 8.1 Traceability (summary)
| Requirements | Module / owner | Tests |
|---|---|---|
| FR-AUTH-01…10, NFR-SEC-02/06/08/10 | AUTH / Nabeeh (@nabosallem-svg) | `AuthServiceTest`, `AccountSecurityTest`, ZAP auth scan, brute-force test |
| FR-CAT-01…08, NFR-SEC-03/09 | CATALOG / Hoda (@hodayasser511-stack) | `CatalogTest`, Selenium browse/search, SQLi & upload tests |
| FR-ORD-01…13, NFR-SEC-01/05 | ORDER / Abdelrhman (@abdelrhman19122007) | `PricingTest`, `OrderLifecycleTest`, Newman suite, IDOR & price-tampering tests |
| FR-PAY-01…11 | PAY / Fady (@fadyza15-collab) | `PaymentTest`, `PushServiceTest`, JMeter, replay/amount-tampering tests |
| FR-OPS-01…09 | OPS / Raheek (@Raheek25) | `AdminTest`, `EventHubTest`, Flutter tests, privilege-escalation tests |

### 8.2 Acceptance criteria
The release is accepted when: every **M** requirement passes its test case; no **critical/high** finding remains
open in the security report; coverage ≥ 70 %; the API and both clients run from a clean checkout following the
README.

---

## 9. Team and responsibilities

| Member | Module | Code | Docs & tests |
|---|---|---|---|
| Nabeeh (@nabosallem-svg) — Team lead | AUTH + project setup | auth, users, security, config, CI | SRS §1–2, architecture, final report, static security scans |
| Hoda (@hodayasser511-stack) | CATALOG | stores, products, search, uploads | ERD, Selenium tests, catalogue pentest |
| Abdelrhman (@abdelrhman19122007) | ORDER | cart, orders, zones, lifecycle | sequence/state diagrams, Postman/Newman, order pentest |
| Fady (@fadyza15-collab) | PAY | payments, wallet, promo, notifications | payment section, JMeter, payment pentest |
| Raheek (@Raheek25) | OPS | store, driver, admin, reviews, complaints | test plan & report, Flutter tests, privilege pentest |

Each member owns their module's migration file, code, tests, SRS section and pentest findings, and submits them
through their own pull requests.
