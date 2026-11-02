# Performance tests

**Owner:** Nabeeh (@nabosallem-svg)

Requirement **NFR-PERF-01**: 95 % of API calls answer in < 500 ms with 100 concurrent users.

| File | What |
|---|---|
| `velox-browse.jmx` | Apache JMeter plan: 100 users × 10 rounds of zones → stores → store menu → search, with a 200-status and < 500 ms assertion |
| `quick_load.py` | the same load from Python, for a quick check without JMeter |

```bash
jmeter -n -t tests/performance/velox-browse.jmx -Jhost=localhost -Jport=8080 -l results.jtl -e -o report/
python tests/performance/quick_load.py --base http://localhost:8080 --users 100 --rounds 10
```

## Results
| Date | Where | Load | Requests | Errors | p50 | p95 | max |
|---|---|---|---|---|---|---|---|
| 26 Oct 2026 | team PC (local build, PostgreSQL in Docker) | 100 users × 10 rounds | 4 000 in 3.5 s (≈1 150/s) | 0 | 53 ms | 116 ms | 272 ms |

Within the target on the development PC. _[to fill]_ Repeat with JMeter against the hosted server (1 vCPU)
before the release.
