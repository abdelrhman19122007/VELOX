# VELOX — Test report

**Owner:** Raheek (@Raheek25) — QA & security testing · **Build:** integration of all modules, 26 Oct 2026
Plan: [TEST_PLAN.md](TEST_PLAN.md)

## Summary
| Suite | Result |
|---|---|
| Backend unit + integration (JUnit 5, real PostgreSQL) | **94 / 94 passed**, 86 % instruction coverage |
| Static security — SpotBugs + FindSecBugs | **0 findings** after fixes (blocking in CI) |
| API collection — Newman | **30 / 30 requests, 42 / 42 assertions passed** |
| Mobile — Flutter tests | **6 / 6 passed**; `flutter analyze`: no issues |
| Performance — 100 concurrent users | **p95 116 ms**, 0 errors (target < 500 ms) |
| End-to-end — website and dashboard (Selenium, headless Chrome) | **4 / 4 passed** |
| End-to-end — app flow (manual, browser build) | sign up → order → Fawry sandbox pay → store accepts → driver delivers → live tracking: **passed** |
| CodeQL, Trivy, ZAP baseline | run in CI on GitHub _[to fill after the first run on GitHub]_ |
| Usability (5 people) | _[to fill]_ |
| Manual pentest per module | _[to fill — see security/*.md]_ |

## Re-run — 18 Nov 2026 (app 2.1.10, server on the team PC, database on Supabase)
How to run each suite: [docs/RUN_LOCAL.md](../docs/RUN_LOCAL.md) §8. Raw outputs: [`tests/results/`](results/).

| Suite | Result | Evidence |
|---|---|---|
| Live end-to-end on the real server over HTTPS (`e2e_live_check.py`): pages, security headers, CORS, closed docs, sign-up → order → store accepts/ready → driver takes/picks up/delivers → review → account clean-up | **45 / 45 passed** | `results/2026-11-18-e2e-live.json` |
| Backend unit tests (no Docker needed): CatalogUnitTest 5, OrderStatusTest 3, PasswordHasherTest 3, TokenServiceTest 4 | **15 / 15 passed** | `results/2026-11-18-junit-unit.txt` |
| Backend integration tests (Testcontainers) | **not run** — Docker Desktop was not running on the test PC. Last full run: 94 / 94 (26 Oct) | — |
| Flutter tests + `flutter analyze` | **6 / 6 passed**, no issues | `results/2026-11-18-flutter.txt` |
| Load — 100 users × 10 rounds (`quick_load.py`) | **4 000 requests, 21 errors, p50 981 ms, p95 3 051 ms** — **fails** NFR-PERF-01 (p95 < 500 ms) | `results/2026-11-18-load-100users.txt` |
| Server package (`velox.jar` reading its settings from a file only, website served from the upload folder) | **44 / 45** — the only miss is the HTTPS check, which cannot pass on `localhost` | run on port 8091, 15 Nov |
| One-click local setup (`START-VELOX.bat`, fresh Docker database): `.env` with random secrets → db + api healthy → demo loader (4 stores, 111/111 dishes, store/driver/customer accounts) → pages, photos and phone link (LAN IP) answer → order ORD-1001 placed → accepted → ready → taken → picked up → **DELIVERED** → reviewed; admin API without token **401**, with a customer token **403**, SQL-injection login **401** | **passed** — except the Maven build inside Docker, which could not download on the test PC because Avast scans HTTPS (the test used the same runtime image with a JAR built on the host) | 18 Nov |
| Manual — driver screen (ORD-1009) | order → store accepts and marks ready → driver takes, picks up, delivers; earnings 15 EGP: **passed** | 12 Nov |
| Manual — store screen (ORD-1011) | new order rings and shows without refresh → Accept → Ready from the UI: **passed**; no driver online overnight, so `StaleOrderCloser` cancelled it — expected behaviour | 13 Nov |

**Why the load result dropped (D-10):** on 26 Oct the database ran in Docker on the same PC (p95 116 ms). Now every query
goes over the internet to Supabase (Stockholm) through a pooler with a small connection limit, so requests queue for a
database connection; the 21 errors are time-outs under that queue. Next steps: test again with the local Docker
database to confirm, then tune the pool (`spring.datasource.hikari.maximum-pool-size`) and cache the store list.

## Defects found and fixed during this cycle
| # | Defect | Severity | Fix |
|---|---|---|---|
| D-01 | Unlimited guessing of the Vodafone Cash code (attempt counter rolled back with the error) | High | counter survives the error; regression test |
| D-02 | SQL strings built at run time in 6 places (constants only — not exploitable, flagged by FindSecBugs) | Low | all SQL is fixed text |
| D-03 | Order insert could leak a JDBC statement on error; possible null id | Low | `INSERT … RETURNING id` |
| D-04 | Wallet-paid order response still said "waiting for payment" | Low | order re-read after paying |
| D-05 | App: payment screen used the page context after it closed; tracking stopped receiving live updates | Medium | screens keep their own reference; several screens can follow one order |
| D-06 | App: "View cart" from a store page didn't switch to the cart tab | Low | the tab lives in the app state |
| D-07 | Dashboard: wide tables pushed the page sideways on small screens | Low | tables scroll inside their box |
| D-08 | App: the server link could be hijacked through the public ntfy topic (any https link was accepted) | **Critical** | only `https://…trycloudflare.com` accepted; release builds lock the link with `--dart-define=API_URL` |
| D-09 | App: store and driver screens skipped the refresh when an action answered with an empty body | Low | always reload after an action (found while testing ORD-1011) |
| D-10 | Load test p95 3 s with the online database (see the 18 Nov re-run) | Medium | **open** |
| D-11 | `deploy/start-public.ps1` cannot run on Windows PowerShell 5.1 (UTF-8 dashes read as quotes), and Avast quarantines it | Low | **open** — manual steps in `docs/RUN_LOCAL.md`; script fix waits for an antivirus exception |
| D-12 | Android: cleartext HTTP allowed and app data (with the session token) included in backups | High | `usesCleartextTraffic=false`, `allowBackup=false` in release |

## Evidence
- CI runs on GitHub Actions (`backend`, `security` workflows).
- `tests/api/README.md` — Newman output summary.
- `tests/performance/README.md` — load numbers.
- Screenshots: `security/evidence/` and `design/screens/` _[to fill]_.
