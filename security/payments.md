# Penetration test report — Payments, wallet & notifications module

| | |
|---|---|
| Module | Payments (Vodafone Cash, Fawry, wallet, refunds, promo codes, loyalty) and notifications |
| Tester | Fady (@fadyza15-collab) |
| Date | _fill in_ |
| Tools | Burp Suite Community (Repeater, Intruder, Turbo/parallel send), OWASP ZAP, curl, automated tests (`backend/src/test/java/com/velox/payment`) |
| Target | local build of branch `feature/payments` (`http://localhost:8080`) |

## Scope
`/api/orders/{code}/payment`, `/api/orders/{code}/payment/confirm`, `/api/orders/{code}/payment/sandbox-pay`,
`/api/me/wallet`, `/api/me/wallet/sandbox-top-up`, `/api/admin/promos`, the `promo` and `paymentMethod` fields of `/api/orders`,
`/api/me/notifications`, `/api/me/notifications/read`, `/api/me/devices`, `/api/me/devices/remove`, `/api/push-config`.

## Test cases
Run each case yourself, record the request and response, and save screenshots in `security/evidence/payments/`.

| # | Attack (OWASP) | How to test | Expected | Automated | Result |
|---|---|---|---|---|---|
| P-01 | Brute-forcing the SMS code (A07) | Intruder on `/payment/confirm` with 000000–999999 | 5 × WRONG_CODE, then TOO_MANY_ATTEMPTS for every code, including the right one | `OnlinePaymentApiTest` | _pass/fail_ |
| P-02 | Payment replay (A04) | repeat a successful `/payment/confirm` or `/sandbox-pay` in Repeater | PAYMENT_CLOSED, order paid once | `OnlinePaymentApiTest` | |
| P-03 | IDOR — read another customer's payment (A01) | customer B: `GET /api/orders/{A's code}/payment` | 404 ORDER_NOT_FOUND (same as a code that doesn't exist) | `OnlinePaymentApiTest` | |
| P-04 | IDOR — pay / confirm another customer's order (A01) | customer B: `sandbox-pay` / `confirm` on A's order | 404, A's order unchanged | `OnlinePaymentApiTest` | |
| P-05 | Amount tampering (A04) | add `"amount": 1` / `"total": 1` to the order or to `/confirm` | ignored, the charge equals the server's total | — (manual) | |
| P-06 | Paying after the time ran out (A04) | wait 10 min (Vodafone Cash) or set `expires_at` in the past, then confirm | PAYMENT_EXPIRED, the order is cancelled | `OnlinePaymentApiTest` | |
| P-07 | Double spend from the wallet — race (A04) | balance 120; send two 115-EGP WALLET orders at the same instant (Burp "send group in parallel") | exactly one succeeds, balance never below 0 | — (manual) | |
| P-08 | Wallet top-up abuse (A04) | `amount: -500`, `0`, `5000`, `1e9`, `"abc"` | 400 | `WalletAndPromoApiTest` | |
| P-09 | Refund abuse (A04) | cancel / return the same paid order twice; return a cash order | one refund only; nothing for cash | `WalletAndPromoApiTest` | |
| P-10 | Promo reuse (A04) | use VELOX10 twice from one account; use a max-1 code from two accounts | INVALID_PROMO the second time | `WalletAndPromoApiTest` | |
| P-11 | Promo enumeration (A07) | Intruder on the `promo` field with a word list | INVALID_PROMO for every failure reason, no difference in the response | `WalletAndPromoApiTest` | |
| P-12 | Customer creates a promo / reads all promos (A01) | customer token on `/api/admin/promos` | 403 | `WalletAndPromoApiTest` | |
| P-13 | Unpaid order reaches the store (A04) | place a FAWRY order without paying; check the store's inbox and orders | nothing until paid | `NotificationApiTest` | |
| P-14 | Reading / removing other users' notifications or devices (A01) | `devices/remove` with another account's token | own data only | `NotificationApiTest` | |
| P-15 | Secrets leak (A02, A05) | search the repo and the API responses for `private_key`, `service-account`; check `/api/push-config` | only public Firebase settings; the service account is never served or committed | — (manual) | |
| P-16 | SMS code stored in clear (A02) | `SELECT code_hash FROM payments` | PBKDF2 hash, not the 6 digits | — (manual) | |
| P-17 | Sandbox endpoints in production (A05) | run with `velox.payments.sandbox=false`; call `sandbox-pay` and `sandbox-top-up` | 404 | — (manual) | |

## Found and fixed during development
| Finding | Severity | Fix |
|---|---|---|
| A wrong SMS code threw inside the transaction, which rolled back the attempt counter, so the 5-try limit never triggered (unlimited guessing of a 6-digit code) | High | `@Transactional(noRollbackFor = ApiException.class)` on `PaymentService.confirmWallet`; covered by `fiveWrongCodesClosesThePayment` |
| The order returned right after a wallet payment still said PENDING_PAYMENT | Low | `OrderService.place` re-reads the order after the payment starts |

## Findings
_List anything you find: endpoint, request, response, impact, severity (CVSS), and the fix._
