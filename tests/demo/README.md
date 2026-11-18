# Demo scripts

Make a realistic order so you can watch the store and driver screens work. Run against your own server only.

```powershell
$env:VELOX_URL = 'http://localhost:8080'
$env:VELOX_ACCOUNTS_FILE = '..\..\..\_secrets\VELOX-accounts.txt'   # staff accounts, kept outside the repo
python tests\demo\demo_order.py     # test customer orders from Nasef (R1); the store accepts and marks it ready
#   -> open the driver screen: the order is waiting for a driver
python tests\demo\demo_cleanup.py   # deletes the test customer and blocks any leftover demo customers
```

`demo_order.py` keeps the test customer's login in `demo_state.json` next to it (git-ignored) so the clean-up can delete
that account. The admin login for the clean-up is read from `deploy/.env`.
