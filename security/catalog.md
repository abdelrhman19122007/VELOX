# Penetration test report — Catalogue module

| | |
|---|---|
| Module | Catalogue (stores, products, search, uploads, favourites) |
| Tester | Hoda Yasser (@hodayasser511-stack) |
| Date | _fill in_ |
| Tools | Burp Suite Community, OWASP ZAP, curl, automated tests (`backend/src/test/java/com/velox/catalog`) |
| Target | local build of branch `feature/catalog` (`http://localhost:8080`) |

## Scope
`/api/stores`, `/api/stores/{id}`, `/api/search`, `/api/me/favorites`, `/api/restaurant/store`, `/api/restaurant/products/{id}`,
`/api/admin/stores…`, `/api/admin/products/{id}`, `/api/uploads`, `/uploads/*`.

## Test cases
Run each case yourself (Burp Repeater / curl), record what you sent and what came back, and attach a screenshot in
`security/evidence/catalog/`. The "Automated" column is the test that already checks the same thing on every build.

| # | Attack (OWASP) | How to test | Expected | Automated | Result |
|---|---|---|---|---|---|
| C-01 | SQL injection in search (A03) | `GET /api/search?q=' OR '1'='1` · `q=%27;DROP TABLE stores;--` | 200, empty result, no SQL error | `SearchAndFavoritesApiTest` | _pass/fail_ |
| C-02 | Wildcard abuse (A03) | `q=%25%25`, `q=__` | no results (not the whole catalogue) | `SearchAndFavoritesApiTest` | |
| C-03 | Stored XSS in product/store name (A03) | admin creates a product named `<img src=x onerror=alert(1)>` | 400 INVALID_REQUEST | `StoreApiTest` | |
| C-04 | Malicious upload (A08) | upload an `.html` / `.svg` / `.php` renamed to `.png` | 400 NOT_AN_IMAGE | `UploadApiTest` | |
| C-05 | Path traversal in file name (A01) | upload with filename `../../evil.png` | saved under a random name inside /uploads | `UploadApiTest` | |
| C-06 | Oversized upload / DoS (A04) | 5 MB image | 400 FILE_TOO_LARGE | `UploadApiTest` | |
| C-07 | Uploaded file served as HTML (A05) | open an uploaded image URL, check headers | `Content-Type: image/*`, `X-Content-Type-Options: nosniff` | `UploadApiTest` | |
| C-08 | IDOR: store edits another store's product (A01) | sign in as store A, `PATCH /api/restaurant/products/{product of B}` | 404, price unchanged | `StoreApiTest` | |
| C-09 | Privilege escalation: customer uses admin/store endpoints (A01) | customer token on `POST /api/admin/stores`, `PATCH /api/restaurant/store` | 403 | `StoreApiTest` | |
| C-10 | Unauthenticated upload (A07) | `POST /api/uploads` without token | 401 | `UploadApiTest` | |
| C-11 | Price tampering by store (A04) | `PATCH …/products/{id}` with `price: -5`, `0`, `9999999999` | 400 | — (manual) | |
| C-12 | Favourites of another user (A01) | user B lists favourites after user A added some | B sees only their own | `SearchAndFavoritesApiTest` | |
| C-13 | Information leakage (A05) | send broken JSON / unknown store id | generic error, no stack trace | — (manual) | |

## Findings
| ID | Title | Severity (CVSS 3.1) | Status |
|---|---|---|---|
| _none yet_ | | | |

For each finding: description, steps to reproduce, evidence, impact, fix, retest result.

## Conclusion
_Summary of what was tested, what was found and fixed, and remaining risk._
