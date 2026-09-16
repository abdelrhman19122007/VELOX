-- VELOX V12 migration: coupon system columns + design codes (run once).
USE velox_db;

ALTER TABLE promo_codes ADD COLUMN free_shipping TINYINT(1) NOT NULL DEFAULT 0;

INSERT INTO promo_codes (code, description, discount_percentage, min_order_amount, max_uses, used_count, is_active, expires_at, free_shipping) VALUES
('CASHBACK25', '25% cashback weekend', 25.00, 300.00, 500, 0, 1, NULL, 0),
('SHW-FREE', 'Free shawarma delivery', 0.00, 150.00, 500, 0, 1, NULL, 1),
('VELOX50', '50% off mega deal', 50.00, 500.00, 100, 0, 1, NULL, 0);
