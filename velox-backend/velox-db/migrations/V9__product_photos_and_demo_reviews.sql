-- VELOX V9 migration: real local product photos (run once).
-- Replaces foreign hotlink images with verified Wikimedia Commons photos
-- stored under velox-frontend/assets/images/products/ + demo store reviews.
USE velox_db;

UPDATE products SET image_url = 'assets/images/products/p06-grill.jpg' WHERE id = 6;
UPDATE products SET image_url = 'assets/images/products/p07-shawarma.jpg' WHERE id = 7;
UPDATE products SET image_url = 'assets/images/products/p10-pizza.jpg' WHERE id = 10;
UPDATE products SET image_url = 'assets/images/products/p11-burger.jpg' WHERE id = 11;
UPDATE products SET image_url = 'assets/images/products/p13-pasta.jpg' WHERE id = 13;
UPDATE products SET image_url = 'assets/images/products/p18-polo.jpg' WHERE id = 18;
UPDATE products SET image_url = 'assets/images/products/p24-macbook.jpg' WHERE id = 24;
UPDATE products SET image_url = 'assets/images/products/p26-watch.jpg' WHERE id = 26;
UPDATE products SET image_url = 'assets/images/products/p28-pancakes.jpg' WHERE id = 28;
UPDATE products SET image_url = 'assets/images/products/p29-burger.jpg' WHERE id = 29;
UPDATE products SET image_url = 'assets/images/products/p30-pizza.jpg' WHERE id = 30;
UPDATE products SET image_url = 'assets/images/products/p32-salad.jpg' WHERE id = 32;
UPDATE products SET image_url = 'assets/images/products/p36-sneakers.jpg' WHERE id = 36;
UPDATE products SET image_url = 'assets/images/products/p39-headphones.jpg' WHERE id = 39;
UPDATE products SET image_url = 'assets/images/products/p40-camera.jpg' WHERE id = 40;

-- Demo store ratings: one delivered showcase order + review per store
-- (real customer reviews accumulate over these via the app).
INSERT INTO orders (order_code, user_id, address_id, total_amount, delivery_fee, final_amount, status, shipping_address, order_date) VALUES
('DEMO-S1', 1, 1, 120.00, 0, 120.00, 'DELIVERED', 'Damietta', NOW() - INTERVAL 5 DAY),
('DEMO-S2', 1, 1, 450.00, 0, 450.00, 'DELIVERED', 'Damietta', NOW() - INTERVAL 4 DAY),
('DEMO-S3', 1, 1, 11500.00, 0, 11500.00, 'DELIVERED', 'Damietta', NOW() - INTERVAL 6 DAY),
('DEMO-S4', 1, 1, 260.00, 0, 260.00, 'DELIVERED', 'Damietta', NOW() - INTERVAL 3 DAY),
('DEMO-S5', 1, 1, 4500.00, 0, 4500.00, 'DELIVERED', 'Damietta', NOW() - INTERVAL 7 DAY),
('DEMO-S6', 1, 1, 2400.00, 0, 2400.00, 'DELIVERED', 'Damietta', NOW() - INTERVAL 2 DAY),
('DEMO-S7', 1, 1, 950.00, 0, 950.00, 'DELIVERED', 'Damietta', NOW() - INTERVAL 5 DAY);

INSERT INTO order_items (order_id, product_id, quantity, unit_price, subtotal)
SELECT o.id, p.id, 1, p.price, p.price FROM orders o
JOIN products p ON p.id = CASE o.order_code
  WHEN 'DEMO-S1' THEN 3 WHEN 'DEMO-S2' THEN 4 WHEN 'DEMO-S3' THEN 5
  WHEN 'DEMO-S4' THEN 10 WHEN 'DEMO-S5' THEN 17 WHEN 'DEMO-S6' THEN 20
  ELSE 27 END
WHERE o.order_code LIKE 'DEMO-S%';

INSERT INTO reviews (user_id, order_id, rating, comment)
SELECT 1, o.id,
  CASE o.order_code WHEN 'DEMO-S2' THEN 4 WHEN 'DEMO-S6' THEN 4 ELSE 5 END,
  CASE o.order_code
    WHEN 'DEMO-S1' THEN 'أكل ممتاز وتوصيل سريع'
    WHEN 'DEMO-S2' THEN 'خامات كويسة والأسعار مناسبة'
    WHEN 'DEMO-S3' THEN 'أجهزة أصلية وضمان حقيقي'
    WHEN 'DEMO-S4' THEN 'أحلى بيتزا والتوصيل سخن'
    WHEN 'DEMO-S5' THEN 'تغليف شيك ومنتجات أصلية'
    WHEN 'DEMO-S6' THEN 'مقاسات مظبوطة وخامة نضيفة'
    ELSE 'شاحن سريع وتعامل راقي'
  END
FROM orders o WHERE o.order_code LIKE 'DEMO-S%';
