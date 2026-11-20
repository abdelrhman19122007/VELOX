# Penetration test report — Operations module (store, driver, admin, reviews, complaints)

| | |
|---|---|
| Module | Operations: store dashboard, driver app, admin dashboard, reviews, complaints |
| Tester | Raheek (@Raheek25) |
| Date | _fill in_ |
| Tools | Burp Suite Community (Repeater, Intruder, parallel send), OWASP ZAP, curl, automated tests (`backend/src/test/java/com/velox/operation`) |
| Target | local build of branch `feature/operations` (`http://localhost:8080`) |

## Scope
`/api/restaurant/orders`, `/api/restaurant/orders/{code}/accept|reject|ready`, `/api/restaurant/stats`, `/api/restaurant/reviews`,
`/api/driver/status`, `/api/driver/location`, `/api/driver/offers`, `/api/driver/orders`, `/api/driver/orders/{code}/take|pickup|deliver`,
`/api/driver/earnings`, `/api/admin/orders`, `/api/admin/orders/{code}/cancel`, `/api/admin/stats`, `/api/admin/complaints`,
`/api/admin/complaints/{code}/close`, `/api/orders/{code}/review`, `/api/orders/{code}/complaint`, `/api/me/complaints`.

Focus of this module: **privilege escalation** (a customer or driver using store/admin functions) and **business-logic abuse**.

## Test cases
Run each case yourself, record the request and response, and save screenshots in `security/evidence/operations/`.

| # | Attack (OWASP) | How to test | Expected | Automated | Result |
|---|---|---|---|---|---|
| OP-01 | Vertical privilege escalation (A01) | customer and driver tokens on every `/api/admin/*` and `/api/restaurant/*` endpoint | 403 | `RestaurantOrderApiTest`, `AdminAndFeedbackApiTest` | _pass/fail_ |
| OP-02 | Self-registering as admin / store (A01) | `POST /api/auth/register` with `"role": "ADMIN"` or `"RESTAURANT"` | refused | auth tests | |
| OP-03 | Store acts on another store's order (A01) | store X: accept / reject / ready on store Y's order code | 404 | `RestaurantOrderApiTest` | |
| OP-04 | Store sees unpaid orders (A04) | place a FAWRY order without paying, look at the store list, try accept | hidden, 404 | `RestaurantOrderApiTest` | |
| OP-05 | Driver acts on another driver's order (A01) | driver B: pickup / deliver on A's order | 404 | `DriverApiTest` | |
| OP-06 | Two drivers take the same order — race (A04) | send `take` from two drivers at the same instant (Burp "send group in parallel") | exactly one succeeds | `DriverApiTest` (sequential) + manual | |
| OP-07 | Hoarding orders (A04) | one driver takes several orders | BUSY after the first | `DriverApiTest` | |
| OP-08 | Skipping steps (A04) | driver picks up before READY, delivers before pickup; store readies before accepting | BAD_STATUS | `DriverApiTest` | |
| OP-09 | Vehicle check bypass (A04) | motorbike driver takes an order with LARGE items | NEEDS_TUKTUK | `DriverApiTest` | |
| OP-10 | Offline driver gets offers (A04) | go offline, call `/api/driver/offers` and `take` | NOT_ONLINE | `DriverApiTest` | |
| OP-11 | Fake location (A04) | `lat: 200`, `lng: "abc"` | 400 | `DriverApiTest` | |
| OP-12 | Review abuse (A04) | review before delivery, twice, someone else's order, rating 0 / 6 / -1 | NOT_DELIVERED / ALREADY_REVIEWED / 404 / 400 | `AdminAndFeedbackApiTest` | |
| OP-13 | Stored XSS in reviews, complaints, reject reasons (A03) | `<script>alert(1)</script>` in comment, details, reason | 400 | `AdminAndFeedbackApiTest`, `RestaurantOrderApiTest` | |
| OP-14 | Double compensation (A04) | close the same complaint twice (also in parallel) | COMPLAINT_CLOSED, one code only | `AdminAndFeedbackApiTest` + manual | |
| OP-15 | Using someone else's compensation code (A01) | customer B uses A's SORRY… code | INVALID_PROMO | `AdminAndFeedbackApiTest` | |
| OP-16 | Injection in filters (A03) | `/api/admin/orders?status=' OR 1=1--`, `?status=DROP TABLE` | 400 | `AdminAndFeedbackApiTest` | |
| OP-17 | Repudiation (A09) | admin cancels an order — is it in `audit_log` with who and why? | yes | `AdminAndFeedbackApiTest` | |
| OP-18 | Driver data exposure (A01) | as a customer, look at the order JSON — only the driver's name, phone and location while delivering? any password hash, email, token? | no secrets | — (manual) | |

## Findings
_List anything you find: endpoint, request, response, impact, severity (CVSS), and the fix._
