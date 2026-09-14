# VELOX integration checklist

1. Add/enable an HTTP layer in front of the Java DAOs.
2. Set `USE_MOCK_API: false` in `js/config.js`.
3. Match the auth response shape documented in the original project README or return a compatible `{ success, token, user }` object.
4. Expose `/products` and `/categories` for the storefront catalog.
5. Expose `/orders` to persist the cart checkout.
6. Replace the demo order confirmation with the real order response and order tracking page.
7. Add server-side authorization and validation for every order.
