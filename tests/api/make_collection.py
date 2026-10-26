"""Builds velox.postman_collection.json (run: python tests/api/make_collection.py). Edit the requests here, not the JSON."""
import json
import pathlib
import uuid


def req(name, method, path, body=None, auth=None, tests=None, pre=None):
    clean, _, query = path.partition('?')
    r = {"name": name, "request": {"method": method, "header": [{"key": "Content-Type", "value": "application/json"}],
                                    "url": {"raw": "{{baseUrl}}" + path, "host": ["{{baseUrl}}"],
                                            "path": [p for p in clean.strip('/').split('/') if p]}}}
    if query:
        r["request"]["url"]["query"] = [{"key": kv.split('=')[0], "value": kv.split('=')[1]} for kv in query.split('&')]
    if body is not None:
        r["request"]["body"] = {"mode": "raw", "raw": json.dumps(body, ensure_ascii=False, indent=2)}
    if auth:
        r["request"]["auth"] = {"type": "bearer", "bearer": [{"key": "token", "value": "{{" + auth + "}}", "type": "string"}]}
    events = []
    if pre:
        events.append({"listen": "prerequest", "script": {"exec": pre.strip().split('\n')}})
    if tests:
        events.append({"listen": "test", "script": {"exec": tests.strip().split('\n')}})
    if events:
        r["event"] = events
    return r


def status(c, extra=''):
    return f'pm.test("status {c}", () => pm.response.to.have.status({c}));\n' + extra


def code(c):
    return f'pm.test("error code {c}", () => pm.expect(pm.response.json().code).to.eql("{c}"));'


P = "01012345678"
folders = [
    {"name": "1. Public catalogue", "item": [
        req("Zones", "GET", "/api/zones",
            tests=status(200, 'pm.test("Samannoud exists", () => pm.expect(pm.response.json().map(z => z.code)).to.include("SAMANNOUD"));')),
        req("Stores", "GET", "/api/stores",
            tests=status(200, 'const s = pm.response.json();\npm.test("array", () => pm.expect(s).to.be.an("array"));\nif (s.length) pm.collectionVariables.set("storeId", s[0].id);')),
        req("Store detail", "GET", "/api/stores/{{storeId}}",
            tests=status(200, 'const p = pm.response.json().sections.flatMap(x => x.products).filter(p => p.available);\nif (p.length) pm.collectionVariables.set("productId", p[0].id);')),
        req("Search with SQL wildcards (A03)", "GET", "/api/search?q=%25%27--", tests=status(200)),
        req("OpenAPI description", "GET", "/v3/api-docs", tests='pm.test("200 in dev, 404 when docs are off", () => pm.expect(pm.response.code).to.be.oneOf([200, 404]));'),
    ]},
    {"name": "2. Accounts & security", "item": [
        req("Register customer A", "POST", "/api/auth/register",
            {"name": "Newman A", "email": "{{emailA}}", "phone": P, "password": "newman-pass-1"},
            pre='pm.collectionVariables.set("emailA", `newman.a.${Date.now()}@example.com`);',
            tests=status(201, 'pm.collectionVariables.set("tokenA", pm.response.json().token);\npm.test("no password in the response", () => pm.expect(pm.response.text()).not.to.include("newman-pass"));')),
        req("Register customer B", "POST", "/api/auth/register",
            {"name": "Newman B", "email": "{{emailB}}", "phone": "01012345679", "password": "newman-pass-2"},
            pre='pm.collectionVariables.set("emailB", `newman.b.${Date.now()}@example.com`);',
            tests=status(201, 'pm.collectionVariables.set("tokenB", pm.response.json().token);')),
        req("Self-register as ADMIN is refused (A01)", "POST", "/api/auth/register",
            {"name": "Evil", "email": "evil{{$timestamp}}@example.com", "phone": "01012345670", "password": "evil-pass-1", "role": "ADMIN"},
            tests='pm.test("refused", () => pm.expect(pm.response.code).to.be.oneOf([400, 403]));'),
        req("Wrong password (A07)", "POST", "/api/auth/login", {"email": "{{emailA}}", "password": "wrong-password"},
            tests='pm.test("refused", () => pm.expect(pm.response.code).to.be.oneOf([400, 401]));\n' + code("BAD_LOGIN")),
        req("Me without token (A01)", "GET", "/api/auth/me", tests=status(401, code("SIGN_IN_REQUIRED"))),
        req("Me with a forged token (A07)", "GET", "/api/auth/me", auth="forged",
            pre='pm.collectionVariables.set("forged", "YWRtaW4.0.9999999999.AAAA");', tests=status(401)),
        req("Customer opens admin numbers (A01)", "GET", "/api/admin/stats", auth="tokenA", tests=status(403, code("FORBIDDEN"))),
        req("Security headers (A05)", "GET", "/api/zones",
            tests='pm.test("nosniff", () => pm.expect(pm.response.headers.get("X-Content-Type-Options")).to.eql("nosniff"));\npm.test("no framing", () => pm.expect(pm.response.headers.get("X-Frame-Options")).to.eql("DENY"));'),
    ]},
    {"name": "3. Ordering", "item": [
        req("Quote — the server prices the cart", "POST", "/api/orders/quote",
            {"zone": "SAMANNOUD", "items": [{"productId": "{{productId}}", "qty": 2}]}, auth="tokenA",
            tests=status(200, 'pm.collectionVariables.set("total", pm.response.json().total);')),
        req("Price tampering is ignored (A04)", "POST", "/api/orders/quote",
            {"zone": "SAMANNOUD", "items": [{"productId": "{{productId}}", "qty": 2, "price": 1}], "total": 1, "deliveryFee": 0}, auth="tokenA",
            tests=status(200, 'pm.test("same total", () => pm.expect(pm.response.json().total).to.eql(pm.collectionVariables.get("total")));')),
        req("Quantity out of range (A04)", "POST", "/api/orders/quote",
            {"zone": "SAMANNOUD", "items": [{"productId": "{{productId}}", "qty": 500}]}, auth="tokenA", tests=status(400)),
        req("Script in the address is refused (A03)", "POST", "/api/orders",
            {"zone": "SAMANNOUD", "phone": P, "address": "<script>alert(1)</script>", "items": [{"productId": "{{productId}}", "qty": 1}], "paymentMethod": "CASH"},
            auth="tokenA", tests=status(400)),
        req("Place a cash order", "POST", "/api/orders",
            {"zone": "SAMANNOUD", "phone": P, "address": "شارع البحر، سمنود", "landmark": "جنب الجامع الكبير",
             "items": [{"productId": "{{productId}}", "qty": 1, "note": "من غير بصل"}], "paymentMethod": "CASH"}, auth="tokenA",
            tests='pm.test("created", () => pm.expect(pm.response.code).to.be.oneOf([200, 201]));\npm.collectionVariables.set("orderCode", pm.response.json().code);'),
        req("Owner reads the order", "GET", "/api/orders/{{orderCode}}", auth="tokenA", tests=status(200)),
        req("IDOR — another customer reads it (A01)", "GET", "/api/orders/{{orderCode}}", auth="tokenB", tests=status(404, code("ORDER_NOT_FOUND"))),
        req("IDOR — another customer cancels it (A01)", "POST", "/api/orders/{{orderCode}}/cancel", auth="tokenB", tests=status(404)),
        req("Owner cancels", "POST", "/api/orders/{{orderCode}}/cancel", auth="tokenA",
            tests=status(200, 'pm.test("cancelled", () => pm.expect(pm.response.json().status).to.eql("CANCELLED"));')),
    ]},
    {"name": "4. Payments & wallet", "item": [
        req("Wallet", "GET", "/api/me/wallet", auth="tokenA", tests=status(200)),
        req("Top-up over the limit (A04)", "POST", "/api/me/wallet/sandbox-top-up", {"amount": 5000}, auth="tokenA", tests=status(400)),
        req("Unknown promo — generic answer (A07)", "POST", "/api/orders/quote",
            {"zone": "SAMANNOUD", "items": [{"productId": "{{productId}}", "qty": 1}], "promo": "NOPE123"}, auth="tokenA",
            tests=status(400, code("INVALID_PROMO"))),
        req("Another customer's payment (A01)", "GET", "/api/orders/{{orderCode}}/payment", auth="tokenB", tests=status(404)),
    ]},
    {"name": "5. Operations", "item": [
        req("Customer opens driver offers (A01)", "GET", "/api/driver/offers", auth="tokenA", tests=status(403)),
        req("Customer opens the store board (A01)", "GET", "/api/restaurant/orders", auth="tokenA", tests=status(403)),
        req("Review before delivery", "POST", "/api/orders/{{orderCode}}/review", {"storeRating": 5}, auth="tokenA",
            tests=status(409, code("NOT_DELIVERED"))),
        req("Notification inbox", "GET", "/api/me/notifications", auth="tokenA", tests=status(200)),
    ]},
]

collection = {
    "info": {"_postman_id": str(uuid.uuid5(uuid.NAMESPACE_URL, "velox-api-tests")),
             "name": "VELOX API — functional and security checks",
             "description": "newman run tests/api/velox.postman_collection.json --env-var baseUrl=http://localhost:8080\n"
                            "Needs one store with an available product (add it from /admin/).",
             "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"},
    "item": folders,
    "variable": [{"key": "baseUrl", "value": "http://localhost:8080"}] +
                [{"key": k, "value": ""} for k in ["storeId", "productId", "emailA", "emailB", "tokenA", "tokenB", "forged", "total", "orderCode"]],
}
out = pathlib.Path(__file__).with_name("velox.postman_collection.json")
out.write_text(json.dumps(collection, ensure_ascii=False, indent=2), encoding="utf-8")
print(f"{sum(len(f['item']) for f in folders)} requests -> {out}")
