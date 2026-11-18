"""Fills a fresh local VELOX with the real menus (demo-data/catalog.json) and makes demo accounts.

Runs once, by itself, inside Docker (service `demo-loader` in docker-compose.yml), or by hand:
    VELOX_URL=http://localhost:8080 VELOX_ADMIN_EMAIL=... VELOX_ADMIN_PASSWORD=... python demo-data/load_demo.py

- Safe to run again: stores that already exist are skipped.
- Only for a LOCAL database. It refuses to touch anything that isn't localhost / the Docker network.
- The new logins are written to demo-data/local-accounts.txt (git-ignored), never printed.
"""
import json
import os
import pathlib
import secrets
import string
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

HERE = pathlib.Path(__file__).resolve().parent
URL = os.environ.get('VELOX_URL', 'http://localhost:8080').rstrip('/')
ADMIN_EMAIL = os.environ.get('VELOX_ADMIN_EMAIL', '').strip()
ADMIN_PASSWORD = os.environ.get('VELOX_ADMIN_PASSWORD', '').strip()
ACCOUNTS = HERE / 'local-accounts.txt'


def say(msg):
    print(msg, flush=True)


def req(method, path, body=None, token=None):
    data = json.dumps(body).encode('utf-8') if body is not None else None
    headers = {'Content-Type': 'application/json; charset=utf-8'}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    r = urllib.request.Request(URL + path, data=data, method=method, headers=headers)
    try:
        with urllib.request.urlopen(r, timeout=60) as x:
            raw = x.read()
            return x.status, (json.loads(raw) if raw else None)
    except urllib.error.HTTPError as e:
        raw = e.read()
        try:
            return e.code, json.loads(raw) if raw else None
        except ValueError:
            return e.code, None


def password(n=14):
    abc = string.ascii_letters + string.digits
    return 'Velox-' + ''.join(secrets.choice(abc) for _ in range(n))


def main():
    host = urllib.parse.urlparse(URL).hostname or ''
    if host not in ('localhost', '127.0.0.1', 'api') and not host.startswith('192.168.'):
        sys.exit(f'Refusing to load demo data into {URL} - this script is for a local VELOX only.')
    if not ADMIN_EMAIL or not ADMIN_PASSWORD:
        sys.exit('Set VELOX_ADMIN_EMAIL and VELOX_ADMIN_PASSWORD (START-VELOX.bat does it for you).')

    say(f'Waiting for VELOX at {URL} ...')
    for _ in range(120):
        try:
            if req('GET', '/api/zones')[0] == 200:
                break
        except OSError:
            pass
        time.sleep(3)
    else:
        sys.exit('VELOX did not answer in 6 minutes.')

    st, r = req('POST', '/api/auth/login', {'email': ADMIN_EMAIL, 'password': ADMIN_PASSWORD})
    if st != 200:
        sys.exit(f'Admin sign-in failed (HTTP {st}). Check VELOX_ADMIN_EMAIL / VELOX_ADMIN_PASSWORD.')
    admin = r['token']

    catalog = json.loads((HERE / 'catalog.json').read_text(encoding='utf-8'))
    existing = {s['name'] for s in (req('GET', '/api/stores')[1] or [])}
    lines = []
    for store in catalog:
        if store['name'] in existing:
            say(f"  = {store['name']}: already there")
            continue
        email = f"{store['id'].lower()}@stores.velox.local"
        pw = password()
        body = {k: store.get(k) for k in ('type', 'cuisine', 'name', 'nameAr', 'tagline', 'taglineAr', 'phone', 'address', 'photoUrl')}
        body.update(accountEmail=email, accountPassword=pw)
        st, r = req('POST', '/api/admin/stores', body, admin)
        if st != 201:
            say(f"  ! {store['name']}: HTTP {st} {r}")
            continue
        sid, added = r['id'], 0
        for p in store['products']:
            pb = {k: p.get(k) for k in ('name', 'nameAr', 'description', 'descriptionEn', 'price', 'size', 'section', 'photoUrl')}
            if req('POST', f'/api/admin/stores/{sid}/products', pb, admin)[0] == 201:
                added += 1
        say(f"  + {store['name']} ({sid}): {added}/{len(store['products'])} dishes")
        lines.append(f"Store  {store['nameAr']} ({sid})\n  email:    {email}\n  password: {pw}\n")

    st, r = req('POST', '/api/admin/users', {'name': 'مندوب تجريبي', 'email': 'driver1@velox.local', 'phone': '01000000001', 'role': 'DRIVER'}, admin)
    if st == 201:
        lines.append(f"Driver  مندوب تجريبي\n  email:    driver1@velox.local\n  password: {r['password']}\n")
        say('  + demo driver')
    elif st == 409:
        say('  = demo driver: already there')

    cpw = password()
    st, _ = req('POST', '/api/auth/register', {'name': 'عميل تجريبي', 'email': 'customer1@velox.local', 'phone': '01000000002', 'password': cpw})
    if st in (200, 201):
        lines.append(f"Customer  عميل تجريبي\n  email:    customer1@velox.local\n  password: {cpw}\n")
        say('  + demo customer')
    elif st == 409:
        say('  = demo customer: already there')

    if lines:
        head = ''
        if not ACCOUNTS.exists():
            head = ('# VELOX local demo accounts - this PC only (git-ignored). Do not share.\n'
                    f'Admin\n  email:    {ADMIN_EMAIL}\n  password: (VELOX_ADMIN_PASSWORD in the .env file next to START-VELOX.bat)\n\n')
        with ACCOUNTS.open('a', encoding='utf-8') as f:
            f.write(head + '\n'.join(lines) + '\n')
        say(f'Logins saved to demo-data/{ACCOUNTS.name}')
    say('Demo data ready.')


if __name__ == '__main__':
    main()
