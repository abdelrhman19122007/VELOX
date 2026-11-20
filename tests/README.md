# Tests

| Folder | What | Owner |
|---|---|---|
| `../backend/src/test` | unit + integration tests (JUnit 5, Testcontainers PostgreSQL) — run on every push | each module owner |
| `api/` | Postman / Newman collection: main flow + security checks | Raheek |
| `e2e/` | Selenium end-to-end tests of the website and dashboard | Raheek, Abdelrhman |
| `../mobile/test` | Flutter tests | Fady |
| `performance/` | JMeter plan and a quick Python load check | Nabeeh |
| `TEST_PLAN.md`, `TEST_REPORT.md` | plan and latest results | Raheek |

Security scans (CodeQL, SpotBugs/FindSecBugs, Trivy, ZAP) run in `.github/workflows/security.yml`;
manual pentest plans are in `../security/`.
