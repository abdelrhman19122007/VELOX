# VELOX — Test plan

**Owner:** Raheek (@Raheek25) — QA & security testing · **Version:** 1.0 · October 2026
Related: [SRS §8](../docs/SRS.md) · [security plans](../security/) · [test report](TEST_REPORT.md)

## 1. Scope
The backend API, the Flutter app (Android and browser), the website and the admin/store dashboard.
Out of scope: real payment gateways (sandbox only), iOS.

## 2. Levels and tools
| Level | Tool | Where | Owner | Runs |
|---|---|---|---|---|
| Unit | JUnit 5 | `backend/src/test` | each module owner | every push (CI) |
| Integration / API (in code) | Spring Boot Test + Testcontainers PostgreSQL | `backend/src/test` | each module owner | every push (CI) |
| API collection | Postman / Newman | `tests/api/` | Raheek | before each release, against a running server |
| UI end-to-end (web) | Selenium WebDriver (Java, JUnit 5) | `tests/e2e/` | Raheek, Abdelrhman | before each release |
| Mobile | Flutter widget tests | `mobile/test/` | Fady | `flutter test` |
| Performance | Apache JMeter | `tests/performance/` | Nabeeh | before release |
| Static security | CodeQL, SpotBugs + FindSecBugs, Trivy | `.github/workflows/security.yml` | Nabeeh | every push + weekly |
| Dynamic security | OWASP ZAP baseline (CI), Burp Suite (manual) | CI + `security/*.md` | everyone, own module | CI + manual |
| Usability | moderated sessions | `design/USABILITY_TEST.md` | Hoda | once before release |

## 3. Environments
- **CI:** GitHub Actions, PostgreSQL in Docker (Testcontainers).
- **Local:** `docker compose up -d db` + `./mvnw spring-boot:run`, app with `flutter run`.
- **Demo / acceptance:** the PC published with `deploy/start-public.ps1` (Cloudflare tunnel).

## 4. Entry and exit criteria
**Entry:** the build is green; the feature is merged to `main`.
**Exit (release):** every **M** requirement passes; all automated suites green; coverage ≥ 70 %;
no open critical/high security finding; usability tasks T1–T6 passed by at least 4 of 5 people.

## 5. Test cases (summary)
Detailed automated cases live in code (test names describe them). Manual and end-to-end cases:

| ID | Area | Case | Expected |
|---|---|---|---|
| E2E-01 | Website | Landing page opens, links to the app and dashboard | page loads, links work |
| E2E-02 | Dashboard | Wrong password | error message, no session |
| E2E-03 | Dashboard | Customer account signs in to the dashboard | refused with a clear message |
| E2E-04 | Dashboard | Admin adds a store and a product, imports a CSV menu | store and items listed |
| E2E-05 | Dashboard | Store staff accepts and readies an order | board columns update live |
| E2E-06 | App | Customer orders cash with a note and change | order placed, store sees the note |
| E2E-07 | App | Fawry order, sandbox pay | order goes to the store |
| E2E-08 | App | Driver takes, picks up, delivers | customer sees each step live |
| E2E-09 | App | Rate and complain after delivery | review shown to the store, complaint in the admin list |
| SEC-* | Security | See `security/<module>.md` | as written there |

## 6. Defect handling
Each defect is a GitHub issue: steps, expected vs actual, severity (critical / high / medium / low), screenshots.
Security defects use CVSS 3.1 and are fixed before release if critical or high, with a regression test.

## 7. Schedule
| Day | Activity |
|---|---|
| D1 | Module PRs merged, CI green, Newman run |
| D2 | Selenium + Flutter tests, ZAP review, manual pentest per module |
| D3 | Usability sessions, fixes |
| D4 | Regression run, test report, release candidate |
