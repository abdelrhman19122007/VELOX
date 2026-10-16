-- VELOX V13 migration: 13 new Egyptian restaurants + 26 products (run once).
-- Covers reuse each store's signature dish photo (local files).
USE velox_db;

INSERT INTO stores (name, name_ar, store_type, phone, address, cover_image, cuisine, branch) VALUES
('Gad', 'جاد', 'RESTAURANT', '01000000008', 'Dokki, Giza', 'assets/images/products/n08-foul.jpg', 'Egyptian, Foul & Taameya', 'Dokki'),
('Abu Shakra', 'أبو شقرة', 'RESTAURANT', '01000000009', 'Nasr City, Cairo', 'assets/images/products/n09-kofta.jpg', 'Grill, Kofta', 'Nasr City'),
('Cook Door', 'كووك دور', 'RESTAURANT', '01000000010', 'Maadi, Cairo', 'assets/images/products/n10-fried.jpg', 'Fried Chicken, Burgers', 'Maadi'),
('Felfela', 'فلفلة', 'RESTAURANT', '01000000011', 'Downtown, Cairo', 'assets/images/products/n11-feteer.jpg', 'Egyptian, Feteer', 'Downtown'),
('TBS Bakery', 'تي بي إس', 'RESTAURANT', '01000000012', 'Zamalek, Cairo', 'assets/images/products/n12-croissant.jpg', 'Bakery, Desserts', 'Zamalek'),
('Mori Sushi', 'موري سوشي', 'RESTAURANT', '01000000013', 'New Cairo', 'assets/images/products/n13-sushi.jpg', 'Japanese, Sushi', 'New Cairo'),
('La Poire', 'لابوار', 'RESTAURANT', '01000000014', 'Heliopolis, Cairo', 'assets/images/products/n14-gateau.jpg', 'Desserts, Patisserie', 'Heliopolis'),
('Hart Attack', 'هارت أتاك', 'RESTAURANT', '01000000015', 'Heliopolis, Cairo', 'assets/images/products/n15-smash.jpg', 'Burgers', 'Heliopolis'),
('Buffalo Burger', 'بافلو برجر', 'RESTAURANT', '01000000016', 'Sheikh Zayed, Giza', 'assets/images/products/n16-hotdog.jpg', 'Burgers, Hot Dogs', 'Sheikh Zayed'),
('Costa Coffee', 'كوستا كوفي', 'RESTAURANT', '01000000017', 'Maadi, Cairo', 'assets/images/products/n17-latte.jpg', 'Coffee, Desserts', 'Maadi'),
('El Prince Seafood', 'البرنس للمأكولات البحرية', 'RESTAURANT', '01000000018', 'Alexandria', 'assets/images/products/n18-fish.jpg', 'Fish & Seafood', 'Alexandria'),
('Abu El Sid', 'أبو السيد', 'RESTAURANT', '01000000019', 'Zamalek, Cairo', 'assets/images/products/n19-mahshi.jpg', 'Lebanese, Oriental', 'Zamalek'),
('Paul Bakery', 'بول', 'RESTAURANT', '01000000020', 'New Cairo', 'assets/images/products/n20-pain.jpg', 'French Bakery', 'New Cairo');

INSERT INTO products (store_id, category_id, name, name_ar, description, description_ar, image_url, price, stock_quantity, product_type, size, is_available) VALUES
((SELECT id FROM stores WHERE name='Gad'), 1, 'Foul Medames', 'فول مدمس', 'Classic foul with olive oil and lemon', 'فول مدمس بزيت الزيتون والليمون', 'assets/images/products/n08-foul.jpg', 35.00, 100, 'FOOD', 'MEDIUM', 1),
((SELECT id FROM stores WHERE name='Gad'), 1, 'Taameya', 'طعمية', 'Crispy taameya 6 pieces', 'طعمية مقرمشة 6 قطع', 'assets/images/products/n08-taameya.jpg', 30.00, 100, 'FOOD', 'MEDIUM', 1),
((SELECT id FROM stores WHERE name='Abu Shakra'), 1, 'Grilled Kofta', 'كفتة مشوية', 'Charcoal grilled kofta half kilo', 'كفتة مشوية على الفحم نصف كيلو', 'assets/images/products/n09-kofta.jpg', 280.00, 40, 'FOOD', 'LARGE', 1),
((SELECT id FROM stores WHERE name='Abu Shakra'), 1, 'Grilled Chicken', 'فراخ مشوية', 'Whole charcoal grilled chicken', 'فرخة كاملة مشوية على الفحم', 'assets/images/products/n09-chicken.jpg', 220.00, 40, 'FOOD', 'LARGE', 1),
((SELECT id FROM stores WHERE name='Cook Door'), 1, 'Crispy Chicken', 'دجاج مقرمش', 'Crispy chicken 8 pieces bucket', 'دجاج مقرمش 8 قطع', 'assets/images/products/n10-fried.jpg', 310.00, 50, 'FOOD', 'LARGE', 1),
((SELECT id FROM stores WHERE name='Cook Door'), 1, 'French Fries', 'بطاطس مقلية', 'Golden crispy fries', 'بطاطس ذهبية مقرمشة', 'assets/images/products/n10-fries.jpg', 60.00, 80, 'FOOD', 'MEDIUM', 1),
((SELECT id FROM stores WHERE name='Felfela'), 1, 'Feteer Meshaltet', 'فطير مشلتت', 'Layered feteer with honey', 'فطير مشلتت بالعسل', 'assets/images/products/n11-feteer.jpg', 95.00, 50, 'FOOD', 'LARGE', 1),
((SELECT id FROM stores WHERE name='Felfela'), 1, 'Meat Fatta', 'فتة باللحمة', 'Traditional fatta with meat', 'فتة بلدي باللحمة', 'assets/images/products/n11-fatta.jpg', 180.00, 40, 'FOOD', 'LARGE', 1),
((SELECT id FROM stores WHERE name='TBS Bakery'), 1, 'Butter Croissant', 'كرواسون بالزبدة', 'French butter croissant', 'كرواسون فرنسي بالزبدة', 'assets/images/products/n12-croissant.jpg', 55.00, 70, 'FOOD', 'SMALL', 1),
((SELECT id FROM stores WHERE name='TBS Bakery'), 1, 'Glazed Donuts', 'دونتس', 'Glazed donuts 4 pieces', 'دونتس 4 قطع', 'assets/images/products/n12-donuts.jpg', 120.00, 60, 'FOOD', 'MEDIUM', 1),
((SELECT id FROM stores WHERE name='Mori Sushi'), 1, 'Sushi Set', 'سوشي سيت', 'Chef special sushi set 18 pieces', 'سوشي سيت 18 قطعة', 'assets/images/products/n13-sushi.jpg', 650.00, 20, 'FOOD', 'LARGE', 1),
((SELECT id FROM stores WHERE name='Mori Sushi'), 1, 'Salmon Sashimi', 'ساشيمي سلمون', 'Fresh salmon sashimi', 'ساشيمي سلمون طازج', 'assets/images/products/n13-salmon.jpg', 480.00, 20, 'FOOD', 'MEDIUM', 1),
((SELECT id FROM stores WHERE name='La Poire'), 1, 'Chocolate Gateau', 'جاتوه شوكولاتة', 'Belgian chocolate gateau', 'جاتوه شوكولاتة بلجيكي', 'assets/images/products/n14-gateau.jpg', 380.00, 25, 'FOOD', 'MEDIUM', 1),
((SELECT id FROM stores WHERE name='La Poire'), 1, 'Chocolate Eclair', 'إكلير شوكولاتة', 'French eclair with dark glaze', 'إكلير فرنسي بالشوكولاتة', 'assets/images/products/n14-eclair.jpg', 65.00, 60, 'FOOD', 'SMALL', 1),
((SELECT id FROM stores WHERE name='Hart Attack'), 1, 'Double Smash Burger', 'دبل سماش برجر', 'Double smashed beef burger', 'دبل برجر لحم مدخن', 'assets/images/products/n15-smash.jpg', 215.00, 50, 'FOOD', 'MEDIUM', 1),
((SELECT id FROM stores WHERE name='Hart Attack'), 1, 'Crispy Fries', 'بطاطس مقرمشة', 'Extra crispy fries', 'بطاطس مقرمشة زيادة', 'assets/images/products/n15-fries.jpg', 70.00, 70, 'FOOD', 'MEDIUM', 1),
((SELECT id FROM stores WHERE name='Buffalo Burger'), 1, 'Beef Burger', 'برجر لحم', 'Classic beef burger', 'برجر لحم كلاسيك', 'assets/images/products/n16-burger.jpg', 165.00, 50, 'FOOD', 'MEDIUM', 1),
((SELECT id FROM stores WHERE name='Buffalo Burger'), 1, 'Hot Dog', 'هوت دوج', 'Grilled hot dog sandwich', 'ساندوتش هوت دوج مشوي', 'assets/images/products/n16-hotdog.jpg', 95.00, 60, 'FOOD', 'MEDIUM', 1),
((SELECT id FROM stores WHERE name='Costa Coffee'), 1, 'Caffe Latte', 'لاتيه', 'Creamy caffe latte', 'لاتيه بالحليب', 'assets/images/products/n17-latte.jpg', 85.00, 90, 'FOOD', 'SMALL', 1),
((SELECT id FROM stores WHERE name='Costa Coffee'), 1, 'Cappuccino', 'كابتشينو', 'Classic cappuccino', 'كابتشينو كلاسيك', 'assets/images/products/n17-cappuccino.jpg', 80.00, 90, 'FOOD', 'SMALL', 1),
((SELECT id FROM stores WHERE name='El Prince Seafood'), 1, 'Grilled Sea Bass', 'سمك قاروص مشوي', 'Grilled sea bass with lemon', 'قاروص مشوي بالليمون', 'assets/images/products/n18-fish.jpg', 340.00, 25, 'FOOD', 'LARGE', 1),
((SELECT id FROM stores WHERE name='El Prince Seafood'), 1, 'Garlic Shrimp', 'جمبري بالثوم', 'Spanish garlic shrimp', 'جمبري بالثوم على الطريقة الإسبانية', 'assets/images/products/n18-shrimp.jpg', 390.00, 25, 'FOOD', 'LARGE', 1),
((SELECT id FROM stores WHERE name='Abu El Sid'), 1, 'Stuffed Vine Leaves', 'محشي ورق عنب', 'Mahshi platter', 'طبق محشي مشكل', 'assets/images/products/n19-mahshi.jpg', 150.00, 40, 'FOOD', 'MEDIUM', 1),
((SELECT id FROM stores WHERE name='Abu El Sid'), 1, 'Shish Tawook', 'شيش طاووق', 'Marinated grilled tawook', 'شيش طاووق متبل ومشوي', 'assets/images/products/n19-shish.jpg', 230.00, 40, 'FOOD', 'MEDIUM', 1),
((SELECT id FROM stores WHERE name='Paul Bakery'), 1, 'Pain au Chocolat', 'بان أو شوكولا', 'French chocolate croissant', 'كرواسون فرنسي بالشوكولاتة', 'assets/images/products/n20-pain.jpg', 75.00, 60, 'FOOD', 'SMALL', 1),
((SELECT id FROM stores WHERE name='Paul Bakery'), 1, 'Club Sandwich', 'كلوب ساندوتش', 'Triple club sandwich', 'كلوب ساندوتش ثلاثي', 'assets/images/products/n20-club.jpg', 140.00, 50, 'FOOD', 'MEDIUM', 1);

-- Showcase delivered orders so new stores show ratings right away.
INSERT INTO orders (order_code, user_id, address_id, total_amount, delivery_fee, final_amount, status, shipping_address, order_date)
SELECT CONCAT('DEMO-N', s.id), 1, 1, 150.00, 0, 150.00, 'DELIVERED', 'Cairo', NOW() - INTERVAL (s.id % 5 + 1) DAY
FROM stores s WHERE s.id >= 8;

INSERT INTO order_items (order_id, product_id, quantity, unit_price, subtotal)
SELECT o.id, m.pid, 1, p.price, p.price
FROM orders o
JOIN stores s ON CONCAT('DEMO-N', s.id) = o.order_code
JOIN (SELECT store_id, MIN(id) AS pid FROM products GROUP BY store_id) m ON m.store_id = s.id
JOIN products p ON p.id = m.pid
WHERE o.order_code LIKE 'DEMO-N%';

INSERT INTO reviews (user_id, order_id, rating, comment)
SELECT 1, o.id, 4 + (o.id % 2), CASE (o.id % 4)
  WHEN 0 THEN 'أكل تحفة والتوصيل كان سريع'
  WHEN 1 THEN 'تجربة جميلة وهطلب تاني'
  WHEN 2 THEN 'الجودة ممتازة والأسعار مناسبة'
  ELSE 'مطعم نضيف والأكل سخن' END
FROM orders o WHERE o.order_code LIKE 'DEMO-N%';
