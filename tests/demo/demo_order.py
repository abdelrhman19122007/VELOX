"""Demo: test customer orders from Nasef (R1), the store accepts and marks it ready -> it shows up for drivers."""
import os, json, sys, time, urllib.request, random

sys.stdout.reconfigure(encoding='utf-8')
U = os.environ.get('VELOX_URL', 'http://localhost:8080')
STATE = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'demo_state.json')


def req(m, p, b=None, tok=None):
    r = urllib.request.Request(U + p, method=m, data=json.dumps(b).encode() if b is not None else None,
                               headers={'Content-Type': 'application/json', **({'Authorization': 'Bearer ' + tok} if tok else {})})
    try:
        with urllib.request.urlopen(r, timeout=40) as x:
            d = x.read()
            return x.status, (json.loads(d) if d else None)
    except urllib.error.HTTPError as e:
        d = e.read()
        return e.code, (json.loads(d) if d else None)


def store_password(email):
    lines = open(os.environ['VELOX_ACCOUNTS_FILE'], encoding='utf-8').read().splitlines()
    for i, l in enumerate(lines):
        if email in l:
            return lines[i + 1].split(':', 1)[1].strip()


pw = 'demo-' + ''.join(random.choice('abcdefghjkmnpqrstuvwxyz23456789') for _ in range(10))
email = f'demo.customer.{int(time.time())}@example.com'
st, r = req('POST', '/api/auth/register', {'name': 'محمد علي (تجربة)', 'email': email, 'phone': '01012345678', 'password': pw})
cust = r['token']
print('customer created', st)

st, menu = req('GET', '/api/stores/R1')
secs = [[p for p in s['products'] if p.get('available', True)] for s in menu['sections']]
prods = [s[0] for s in secs if s][1:3]
print('items:', [p.get('nameAr') or p.get('name') for p in prods])
_, zones = req('GET', '/api/zones')
order = {'storeId': 'R1', 'zone': zones[0]['code'], 'paymentMethod': 'CASH', 'phone': '01012345678',
         'items': [{'productId': prods[0]['id'], 'qty': 2}] + ([{'productId': prods[1]['id'], 'qty': 1}] if len(prods) > 1 else []),
         'address': 'شارع المحطة، عمارة 12، الدور التالت (طلب تجريبي)', 'landmark': 'جنب صيدلية الشفاء',
         'lat': 30.9625, 'lng': 31.2452, 'cashChangeFor': 500}
st, o = req('POST', '/api/orders', order, cust)
print('order', st, o.get('code'), o.get('total'), o.get('code') or o)
code = o['code']

store = req('POST', '/api/auth/login', {'email': 'nasef@stores.velox.local', 'password': store_password('nasef@stores.velox.local')})[1]['token']
print('store accept', req('POST', f'/api/restaurant/orders/{code}/accept', None, store)[0])
print('store ready ', req('POST', f'/api/restaurant/orders/{code}/ready', None, store)[0])
json.dump({'email': email, 'pw': pw, 'code': code}, open(STATE, 'w'))
