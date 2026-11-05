"""Full live check through the public link. Writes results to JSON. Secrets come from local files, never printed.
Test orders are cancelled right away so the restaurant's real numbers stay clean."""
import json, re, secrets, sys, time, urllib.request, urllib.error

sys.stdout.reconfigure(encoding='utf-8')
U = sys.argv[1].rstrip('/')
OUT = sys.argv[2]
ROOT = 'E:/01-Projects/VELOX'
env = dict(l.strip().split('=', 1) for l in open(f'{ROOT}/3-integration-v2/integration/deploy/.env', encoding='utf-8-sig') if '=' in l)
stores_pw = dict(re.findall(r'(\w+)@stores\.velox\.local / (\S+)', open(f'{ROOT}/4-handoff/restaurants/STORE_ACCOUNTS.md', encoding='utf-8').read()))
results = []


def req(method, path, body=None, token=None, raw=False):
    h = {'Content-Type': 'application/json'}
    if token:
        h['Authorization'] = 'Bearer ' + token
    t = time.time()
    try:
        with urllib.request.urlopen(urllib.request.Request(U + path, data=json.dumps(body).encode() if body is not None else None,
                                                           method=method, headers=h), timeout=60) as r:
            b = r.read()
            return r.status, (b if raw else (json.loads(b) if b else None)), time.time() - t
    except urllib.error.HTTPError as e:
        b = e.read()
        try:
            return e.code, json.loads(b), time.time() - t
        except ValueError:
            return e.code, b[:200], time.time() - t


def check(area, name, cond, detail=''):
    results.append({'area': area, 'name': name, 'ok': bool(cond), 'detail': str(detail)})
    print(('PASS' if cond else 'FAIL'), '|', area, '|', name, '|', detail)


# 1 pages
for p, label in [('/', 'Website'), ('/app/', 'App (browser)'), ('/admin/', 'Dashboard'), ('/privacy.html', 'Privacy policy'),
                 ('/delete-account.html', 'Delete-account page'), ('/firebase-messaging-sw.js', 'Browser push worker'),
                 ('/downloads/version.json', 'App update info')]:
    st, _, dt = req('GET', p, raw=True)
    check('Pages', label, st == 200, f'HTTP {st}, {dt:.1f}s')
st, _, dt = req('GET', '/downloads/velox.apk', raw=True)
check('Pages', 'Android app download (20 MB)', st == 200, f'HTTP {st}, {dt:.1f}s')

# 2 public data
st, zones, _ = req('GET', '/api/zones')
check('Catalog', 'Delivery zones', st == 200 and zones, f'{len(zones)} zone(s)')
st, stores, _ = req('GET', '/api/stores')
check('Catalog', 'Stores list', st == 200 and len(stores) == 4, ', '.join(s['name'] for s in stores))
st, nasef, _ = req('GET', '/api/stores/R1')
items = [p for sec in nasef['sections'] for p in sec['products']]
check('Catalog', 'Nasef menu with photos', len(items) == 111 and all(p.get('photoUrl') for p in items), f'{len(items)} dishes')
check('Catalog', 'English descriptions', all(p.get('descriptionEn') for p in items), 'every dish')
st, _, _ = req('GET', nasef['store']['photoUrl'], raw=True)
check('Catalog', 'Store photo loads', st == 200, f'HTTP {st}')
st, rv, _ = req('GET', '/api/stores/R1/reviews')
check('Catalog', 'Public reviews endpoint', st == 200 and isinstance(rv, list), f'{len(rv)} review(s)')
st, sr, _ = req('GET', '/api/search?q=' + urllib.request.quote('كفتة'))
check('Catalog', 'Search ("كفتة")', st == 200, f"{len(sr.get('products', [])) if isinstance(sr, dict) else 0} dish(es)")
st, pc, _ = req('GET', '/api/push-config')
check('Notifications', 'Firebase config served', st == 200 and pc.get('projectId') == 'velox-samannoud' and pc.get('androidAppId'))

# 3 accounts
admin = req('POST', '/api/auth/login', {'email': env['VELOX_ADMIN_EMAIL'], 'password': env['VELOX_ADMIN_PASSWORD']})[1].get('token')
check('Accounts', 'Admin sign-in', admin)
store = req('POST', '/api/auth/login', {'email': 'nasef@stores.velox.local', 'password': stores_pw['nasef']})[1].get('token')
check('Accounts', 'Restaurant (Nasef) sign-in', store)
email, pw = f'velox.check.{secrets.token_hex(3)}@example.com', secrets.token_urlsafe(12)
st, c, _ = req('POST', '/api/auth/register', {'name': 'VELOX Check', 'email': email, 'phone': '01012345678', 'password': pw})
cust = c.get('token') if isinstance(c, dict) else None
check('Accounts', 'Customer sign-up', st in (200, 201) and cust, f'HTTP {st}')
st, me, _ = req('POST', '/api/me/profile', {'name': 'VELOX Checked', 'phone': '01112345678'}, cust)
check('Accounts', 'Edit my name and phone', st == 200 and me.get('name') == 'VELOX Checked', f'HTTP {st}')
st, ad, _ = req('POST', '/api/me/addresses', {'label': 'Home', 'zone': zones[0]['code'], 'address': 'Test street 1', 'landmark': 'next to the mosque', 'phone': '01112345678'}, cust)
check('Accounts', 'Add an address', st == 200 and len(ad) == 1, f'HTTP {st}')

# 4 security
st, _, _ = req('POST', '/api/auth/register', {'name': 'Fake', 'email': 'x' + email, 'phone': '01012345678', 'password': 'test-pass-123', 'role': 'DRIVER'})
check('Security', 'Nobody can sign up as a driver (privilege escalation)', st == 400, f'HTTP {st}')
st, _, _ = req('POST', '/api/auth/register', {'name': 'Fake', 'email': 'y' + email, 'phone': '01012345678', 'password': 'test-pass-123', 'role': 'ADMIN'})
check('Security', 'Nobody can sign up as an admin', st == 400, f'HTTP {st}')
st, _, _ = req('GET', '/api/admin/stats', token=cust)
check('Security', 'Customer blocked from admin API', st == 403, f'HTTP {st}')
st, _, _ = req('GET', '/api/admin/stats')
check('Security', 'No token = no access', st == 401, f'HTTP {st}')
st, _, _ = req('POST', '/api/auth/login', {'email': "' OR 1=1 --", 'password': 'x'})
check('Security', 'SQL injection in sign-in is harmless', st in (400, 401), f'HTTP {st}')
st, _, _ = req('POST', '/api/me/profile', {'name': '<script>alert(1)</script>', 'phone': '01112345678'}, cust)
check('Security', 'HTML/script in a name is refused (XSS)', st == 400, f'HTTP {st}')
st, hdr, _ = req('GET', '/api/zones', raw=True)
check('Security', 'HTTPS link (Cloudflare)', U.startswith('https://'), U.split('/')[2])

# 5 orders and payments
cheap = min((p for p in items if p['available']), key=lambda p: p['price'])
base = {'storeId': 'R1', 'zone': zones[0]['code'], 'items': [{'productId': cheap['id'], 'qty': 2}], 'address': 'check only — cancelled', 'phone': '01112345678'}
st, q, _ = req('POST', '/api/orders/quote', base, cust)
check('Orders', 'Price quote (server computes the price)', st == 200 and abs(q['subtotal'] - 2 * cheap['price']) < 0.01, f"subtotal {q.get('subtotal')}, total {q.get('total')}")
st, q2, _ = req('POST', '/api/orders/quote', {**base, 'items': [{'productId': cheap['id'], 'qty': 2, 'price': 1}], 'total': 1}, cust)
check('Security', 'Price tampering ignored', st == 200 and abs(q2['subtotal'] - 2 * cheap['price']) < 0.01, f"subtotal stays {q2.get('subtotal')}")
for m in ['VODAFONE_CASH', 'FAWRY']:
    st, r, _ = req('POST', '/api/orders', {**base, 'paymentMethod': m, 'walletPhone': '01012345678'}, cust)
    check('Payments', f'{m} switched off', st == 400 and r.get('code') == 'PAYMENTS_UNAVAILABLE', f"HTTP {st} {r.get('code')}")
st, r, _ = req('POST', '/api/me/wallet/sandbox-top-up', {'amount': 100}, cust)
check('Payments', 'Test wallet top-up switched off', st in (403, 404), f'HTTP {st}')
st, o, _ = req('POST', '/api/orders', {**base, 'paymentMethod': 'CASH'}, cust)
code = o.get('code') if isinstance(o, dict) else None
check('Orders', 'Cash order placed', st in (200, 201) and code, f'{code}, status {o.get("status")}')
if code:
    st, so, _ = req('GET', '/api/restaurant/orders', token=store)
    lst = so if isinstance(so, list) else so.get('orders', [])
    check('Orders', 'Order reaches the restaurant', any(x.get('code') == code for x in lst))
    other = req('POST', '/api/auth/register', {'name': 'Other', 'email': 'z' + email, 'phone': '01012345678', 'password': pw})[1]['token']
    st, _, _ = req('GET', f'/api/orders/{code}', token=other)
    check('Security', "Another customer can't see this order (IDOR)", st == 404, f'HTTP {st}')
    st, _, _ = req('POST', f'/api/orders/{code}/cancel', {'reason': 'x'}, other)
    check('Security', "Another customer can't cancel it", st == 404, f'HTTP {st}')
    st, x, _ = req('POST', f'/api/orders/{code}/cancel', {'reason': 'automatic test'}, cust)
    check('Orders', 'Customer cancels own order', st == 200 and x.get('status') == 'CANCELLED', f'HTTP {st}')

# 6 driver (created by admin), account deletion
st, dr, _ = req('POST', '/api/admin/users', {'name': 'Check Driver', 'email': 'd' + email, 'phone': '01112345678', 'role': 'DRIVER'}, admin)
check('Accounts', 'Admin creates a driver account', st == 201 and dr.get('password'), f'HTTP {st}')
if st == 201:
    dtok = req('POST', '/api/auth/login', {'email': dr['email'], 'password': dr['password']})[1].get('token')
    st, _, _ = req('POST', '/api/driver/status', {'online': True, 'vehicle': 'MOTORBIKE'}, dtok)
    check('Operations', 'Driver goes online', st in (200, 204), f'HTTP {st}')
    st, _, _ = req('GET', '/api/driver/offers', token=dtok)
    check('Operations', 'Driver sees offers', st == 200, f'HTTP {st}')
    req('POST', '/api/driver/status', {'online': False}, dtok)
st, _, _ = req('POST', '/api/me/delete', {'password': 'wrong-password'}, cust)
check('Accounts', 'Delete account needs the right password', st == 400, f'HTTP {st}')
st, _, _ = req('POST', '/api/me/delete', {'password': pw}, cust)
check('Accounts', 'Customer deletes their account', st == 204, f'HTTP {st}')
st, _, _ = req('GET', '/api/auth/me', token=cust)
check('Accounts', 'Deleted account is signed out', st == 401, f'HTTP {st}')
for t in [other]:
    req('POST', '/api/me/delete', {'password': pw}, t)

ok = sum(r['ok'] for r in results)
print(f'\nTOTAL {ok}/{len(results)} passed')
json.dump({'url': U, 'at': time.strftime('%Y-%m-%d %H:%M'), 'results': results}, open(OUT, 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
