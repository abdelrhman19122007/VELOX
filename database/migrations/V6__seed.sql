-- =====================================================================
-- V6  Starting data (everything here can be edited from the admin panel)
-- =====================================================================

-- Delivery zones. Only the city itself for now: the team adds the surrounding villages
-- and their fees (admin panel → Zones), from the real list agreed with the drivers.
INSERT INTO delivery_zones (code, name_ar, name_en, kind, fee, eta_min, sort_order) VALUES
    ('SAMANNOUD', 'سمنود', 'Samannoud', 'CITY', 15.00, 25, 1);

-- Launch promo codes.
INSERT INTO promo_codes (code, kind, value, min_order, max_uses) VALUES
    ('VELOX10',  'PERCENT',       10, 50, NULL),
    ('FREESHIP', 'FREE_DELIVERY',  0, 80, NULL),
    ('WELCOME',  'FIXED',         20, 60, NULL);

-- Accounts (admin, demo stores, drivers) are created by the backend on first start with a
-- password from the VELOX_SEED_PASSWORD environment variable — never hard-coded here.
