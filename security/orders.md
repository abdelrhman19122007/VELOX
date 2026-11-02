# Penetration test report — Orders & delivery module

| | |
|---|---|
| Module | Orders (pricing, placing, lifecycle, zones, live updates) |
| Tester | Abdelrhman (@abdelrhman19122007) |
| Date | _fill in_ |
| Tools | Burp Suite Community (Repeater, Intruder), OWASP ZAP, curl, automated tests (`backend/src/test/java/com/velox/order`) |
| Target | local build of branch `feature/orders` (`http://localhost:8080`) |

## Scope
`/api/orders/quote`, `/api/orders`, `/api/orders/mine`, `/api/orders/{code}`, `/api/orders/{code}/cancel`, `/api/orders/{code}/return`,
`/api/zones`, `/api/admin/zones`, `/api/events`, `/api/events/poll`.

## Test cases
Run each case yourself, record the request and response, and save screenshots in `security/evidence/orders/`.

| # | Attack (OWASP) | How to test | Expected | Automated | Result |
|---|---|---|---|---|---|
| O-01 | Price tampering (A04) | add `"total": 1`, `"price": 1`, `"subtotal": 0` to the order body in Burp | ignored, server price used | `OrderApiTest` | _pass/fail_ |
| O-02 | Negative / huge quantity (A04) | `qty: -1`, `0`, `500`, `2147483647` | 400 | `OrderApiTest` | |
| O-03 | IDOR — read another customer's order (A01) | customer B: `GET /api/orders/{A's code}`; try sequential codes ORD-1001, 1002… with Intruder | 404 for all orders that aren't B's | `OrderApiTest` | |
| O-04 | IDOR — cancel another customer's order (A01) | customer B: `POST /api/orders/{A's code}/cancel` | 404, order unchanged | `OrderApiTest` | |
| O-05 | Store reads another store's orders (A01) | store X token on store Y's order | 404 | `OrderApiTest` | |
| O-06 | Skipping lifecycle steps (A04) | store tries PLACED → DELIVERED; customer tries to accept | BAD_STATUS | `OrderLifecycleTest` | |
| O-07 | Race condition (A04) | send accept and cancel at the same instant (Burp "send group in parallel") | exactly one succeeds | — (manual) | |
| O-08 | Mixed-store cart (A04) | items from two stores | MIXED_STORES | `OrderApiTest` | |
| O-09 | Order from a closed store (A04) | store switch off, then order | STORE_CLOSED | `OrderApiTest` | |
| O-10 | XSS in address / note / errand text (A03) | `<script>alert(1)</script>` in address, note, landmark, errandDetails | 400 | — (manual) | |
| O-11 | Unknown zone / fee tampering (A04) | `zone: "MARS"`; send `deliveryFee: 0` | UNKNOWN_ZONE / fee ignored | `OrderApiTest` | |
| O-12 | Non-customer places orders (A01) | store or driver token on `POST /api/orders` | 403 | `OrderApiTest` | |
| O-13 | Live updates of other people (A01) | open `/api/events/poll` with your token, place an order as someone else | you receive nothing | `OrderLifecycleTest` | |
| O-14 | Token in URL (A09) | try `/api/events?token=…` | refused — token only in the header | — (manual) | |
| O-15 | Zone admin by non-admin (A01) | customer `POST /api/admin/zones` | 403 | `OrderApiTest` | |

## Findings
| ID | Title | Severity (CVSS 3.1) | Status |
|---|---|---|---|
| _none yet_ | | | |

## Conclusion
_What was tested, what was found and fixed, remaining risk._
