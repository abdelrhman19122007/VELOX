"""Quick load check without JMeter: N customers browse at once; prints p50/p95/max and errors.
python tests/performance/quick_load.py --base http://localhost:8080 --users 100 --rounds 10"""
import argparse
import concurrent.futures
import statistics
import time
import urllib.request

PATHS = ["/api/zones", "/api/stores", "/api/search?q=%D9%83%D8%B4%D8%B1%D9%8A"]


def browse(base, rounds, extra):
    times, errors = [], 0
    for _ in range(rounds):
        for p in PATHS + extra:
            start = time.perf_counter()
            try:
                with urllib.request.urlopen(base + p, timeout=10) as r:
                    r.read()
                    if r.status != 200:
                        errors += 1
            except Exception:
                errors += 1
            times.append((time.perf_counter() - start) * 1000)
    return times, errors


def main():
    a = argparse.ArgumentParser()
    a.add_argument("--base", default="http://localhost:8080")
    a.add_argument("--users", type=int, default=100)
    a.add_argument("--rounds", type=int, default=10)
    a.add_argument("--store", default="R1")
    args = a.parse_args()
    extra = [f"/api/stores/{args.store}"]
    started = time.perf_counter()
    with concurrent.futures.ThreadPoolExecutor(max_workers=args.users) as pool:
        results = list(pool.map(lambda _: browse(args.base, args.rounds, extra), range(args.users)))
    took = time.perf_counter() - started
    times = sorted(t for r in results for t in r[0])
    errors = sum(r[1] for r in results)
    p = lambda q: times[min(len(times) - 1, int(len(times) * q))]
    print(f"requests {len(times)} in {took:.1f}s ({len(times) / took:.0f}/s), errors {errors}")
    print(f"p50 {p(0.50):.0f} ms · p95 {p(0.95):.0f} ms · max {times[-1]:.0f} ms · mean {statistics.mean(times):.0f} ms")


if __name__ == "__main__":
    main()
