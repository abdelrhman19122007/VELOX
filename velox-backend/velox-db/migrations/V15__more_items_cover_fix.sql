-- VELOX V15 migration: third product per new restaurant + Adidas cover fix (run once).
USE velox_db;

UPDATE stores SET cover_image = 'assets/images/products/ultraboost-running.jpeg' WHERE id = 6;

INSERT INTO products (store_id, category_id, name, name_ar, description, description_ar, image_url, price, stock_quantity, product_type, size, is_available) VALUES
((SELECT id FROM stores WHERE name='Gad'), 1, 'Shakshuka', 'شكشوكة', 'Oriental shakshuka with eggs', 'شكشوكة شرقية بالبيض', 'assets/images/products/m08-shakshuka.jpg', 65.00, 70, 'FOOD', 'MEDIUM', 1),
((SELECT id FROM stores WHERE name='Abu Shakra'), 1, 'Grilled Ribs', 'ريش مشوية', 'Smoky BBQ grilled ribs', 'ريش مشوية على الفحم', 'assets/images/products/m09-ribs.jpg', 350.00, 25, 'FOOD', 'LARGE', 1),
((SELECT id FROM stores WHERE name='Cook Door'), 1, 'Crispy Chicken Burger', 'تشيكن برجر مقرمش', 'Crispy chicken sandwich', 'ساندوتش دجاج مقرمش', 'assets/images/products/m10-chickenburger.jpg', 145.00, 55, 'FOOD', 'MEDIUM', 1),
((SELECT id FROM stores WHERE name='Felfela'), 1, 'Om Ali', 'أم علي', 'Traditional om ali with nuts', 'أم علي بالمكسرات', 'assets/images/products/m11-omali.jpg', 90.00, 50, 'FOOD', 'MEDIUM', 1),
((SELECT id FROM stores WHERE name='TBS Bakery'), 1, 'Cinnamon Rolls', 'سينابون', 'Homemade cinnamon rolls', 'سينابون بالقرفة', 'assets/images/products/m12-cinnabon.jpg', 110.00, 45, 'FOOD', 'MEDIUM', 1),
((SELECT id FROM stores WHERE name='Mori Sushi'), 1, 'Shrimp Tempura', 'تمبورا الجمبري', 'Crispy shrimp tempura', 'تمبورا جمبري مقرمشة', 'assets/images/products/m13-tempura.jpg', 320.00, 25, 'FOOD', 'MEDIUM', 1),
((SELECT id FROM stores WHERE name='La Poire'), 1, 'Birthday Torta', 'تورتة عيد ميلاد', 'Celebration chocolate torta', 'تورتة شوكولاتة للمناسبات', 'assets/images/products/m14-torta.jpg', 550.00, 12, 'FOOD', 'LARGE', 1),
((SELECT id FROM stores WHERE name='Hart Attack'), 1, 'Onion Rings', 'أونيون رينجز', 'Golden onion rings', 'حلقات بصل ذهبية', 'assets/images/products/m15-onion.jpg', 80.00, 65, 'FOOD', 'MEDIUM', 1),
((SELECT id FROM stores WHERE name='Buffalo Burger'), 1, 'Chicken Wings', 'تشيكن وينجز', 'Spicy buffalo wings', 'أجنحة بصوص البافلو', 'assets/images/products/m16-wings.jpg', 185.00, 45, 'FOOD', 'MEDIUM', 1),
((SELECT id FROM stores WHERE name='Costa Coffee'), 1, 'Hot Chocolate', 'هوت شوكلت', 'Rich hot chocolate', 'هوت شوكلت غني', 'assets/images/products/m17-hotchoc.jpg', 95.00, 80, 'FOOD', 'SMALL', 1),
((SELECT id FROM stores WHERE name='El Prince Seafood'), 1, 'Grilled Fillet', 'فيليه مشوي', 'Grilled fish fillet with lemon', 'فيليه مشوي بالليمون', 'assets/images/products/m18-fillet.jpg', 290.00, 30, 'FOOD', 'MEDIUM', 1),
((SELECT id FROM stores WHERE name='Abu El Sid'), 1, 'Adana Kebab', 'كباب أضنة', 'Spicy adana kebab skewers', 'كباب أضنة حار', 'assets/images/products/m19-adana.jpg', 310.00, 30, 'FOOD', 'LARGE', 1),
((SELECT id FROM stores WHERE name='Paul Bakery'), 1, 'Apple Danish', 'دنش بالتفاح', 'Glazed apple danish', 'دنش بالتفاح', 'assets/images/products/m20-danish.jpg', 70.00, 55, 'FOOD', 'SMALL', 1);
