# VELOX — Frontend

Vanilla HTML/CSS/JavaScript storefront prepared to connect to the VELOX Java/MySQL backend later.

## What changed

- `index.html` is now the public storefront. Users can browse categories and products without signing in.
- Login is an account option in the header and opens inside the homepage instead of blocking entry to the store.
- Authentication is required only when the user tries to confirm an order from the cart.
- Added product search with `Ctrl + K` / `Cmd + K` focus shortcut.
- Added category filters, responsive product cards, cart drawer, quantity controls and checkout flow.
- Added light/dark mode and Arabic/English switching.
- VELOX logo is visible in the header, hero treatment, footer and authentication modal; login/register pages retain the brand logo as well.
- Added `catalogService.js` and `orderService.js` as integration points for a future REST API.
- Mock mode remains enabled so the interface can be tested before the HTTP backend exists.

## Main files

```
velox-frontend/
├── index.html                 # public storefront / main entry
├── home.html                  # compatibility storefront route
├── login.html                 # standalone login page
├── register.html              # standalone registration page
├── assets/images/velox-logo.jpeg
├── css/
│   ├── tokens.css
│   ├── base.css
│   ├── components.css
│   ├── auth.css
│   └── home.css
└── js/
    ├── config.js
    ├── i18n.js
    ├── theme.js
    ├── navControls.js
    ├── pages/storefrontPage.js
    ├── pages/loginPage.js
    ├── pages/registerPage.js
    ├── pages/homePage.js
    ├── services/apiClient.js
    ├── services/authService.js
    ├── services/catalogService.js
    ├── services/orderService.js
    └── utils/
        ├── validation.js
        └── domUtils.js
```

## Backend integration points

Set `USE_MOCK_API` to `false` in `js/config.js` after exposing HTTP endpoints.

Current proposed routes:

- `POST /api/auth/login`
- `POST /api/auth/register`
- `GET /api/products`
- `GET /api/categories`
- `POST /api/orders`

The storefront currently expects product objects containing at least:

```json
{
  "id": 1,
  "nameAr": "...",
  "nameEn": "...",
  "storeAr": "...",
  "storeEn": "...",
  "category": "food",
  "price": 145,
  "emoji": "🍔",
  "glow": "#ffe8ef",
  "descAr": "...",
  "descEn": "..."
}
```

The order payload sent to `POST /api/orders` is:

```json
{
  "user_id": 1,
  "items": [
    { "product_id": 1, "quantity": 2 }
  ],
  "total_amount": 290
}
```

## Important

The current mock auth is for frontend testing only. It never stores passwords in browser storage. Real authentication and password hashing must remain a backend responsibility.


## UI behavior updates
- The storefront is accessible without authentication. Authentication is requested only at order confirmation.
- Product search is client-side and searches Arabic/English product names, stores, descriptions and category values. Arabic normalization and light typo tolerance are included.
- Search supports Enter-to-scroll, Escape/clear, a clear button, result count, and works together with category filters.
- Mock authentication now validates the password instead of accepting any non-empty value. Existing mock accounts are migrated safely for the demo.
- Standalone login/register pages return to `index.html` after successful authentication.
- The UI was visually tightened with a softer, friendlier color system, smaller cards, clearer spacing and a redesigned authentication card.
