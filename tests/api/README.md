# API tests (Postman / Newman)

**Owner:** Raheek (@Raheek25)

`velox.postman_collection.json` — 30 requests, 42 assertions: the main customer flow plus security checks
(IDOR, price tampering, quantity abuse, stored XSS, forged token, privilege escalation, generic promo errors,
security headers). It is generated from `make_collection.py`; edit the Python, then run it.

## Run
1. Start the server (`docker compose up -d db` then `cd backend && ./mvnw spring-boot:run`, or `deploy/start-public.ps1`).
2. Add at least one store with an available product from `/admin/`.
3. Run:

```bash
npx newman run tests/api/velox.postman_collection.json --env-var baseUrl=http://localhost:8080
```

Or import the file into Postman and press **Run collection**.

## Last result
26 Oct 2026, local build: **30/30 requests, 42/42 assertions passed** (average response 123 ms).
