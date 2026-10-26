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

## Evidence
- CI runs on GitHub Actions (`backend`, `security` workflows).
- `tests/api/README.md` — Newman output summary.
- `tests/performance/README.md` — load numbers.
- Screenshots: `security/evidence/` and `design/screens/` _[to fill]_.
