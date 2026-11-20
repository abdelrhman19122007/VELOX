"""Clean up after the driver demo: delete the demo customer (own password), block any other demo customer, driver offline."""
import os, json, sys, urllib.request

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
        return e.code, None


st = json.load(open(STATE))
tok = req('POST', '/api/auth/login', {'email': st['email'], 'password': st['pw']})[1]['token']
print('demo customer deleted', req('POST', '/api/me/delete', {'password': st['pw']}, tok)[0])

env = dict(l.split('=', 1) for l in open(os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..', 'deploy', '.env'), encoding='utf-8').read().splitlines() if '=' in l and not l.startswith('#'))
admin = req('POST', '/api/auth/login', {'email': env['VELOX_ADMIN_EMAIL'].strip(), 'password': env['VELOX_ADMIN_PASSWORD'].strip()})[1]['token']
_, users = req('GET', '/api/admin/users', tok=admin)
users = users.get('items', users) if isinstance(users, dict) else users
for u in users:
    if str(u.get('email', '')).startswith('demo.customer.') and not u.get('blocked'):
        print('blocked leftover', u.get('email'), req('PATCH', '/api/admin/users/' + str(u.get('id') or u.get('publicId')), {'blocked': True}, admin)[0])
