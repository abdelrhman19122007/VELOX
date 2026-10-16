-- VELOX V8 migration: extra catalog products (run once).
-- Uses verified local images + stable hotlinks, matching existing rows.
USE velox_db;

INSERT INTO products (store_id, category_id, name, name_ar, description, description_ar, image_url, price, stock_quantity, product_type, size, is_available) VALUES
-- Food: Buffalo & Pizza Hut (store 4, category 1)
('4','1','Chocolate Pancakes','بان كيك بالشوكولاتة','Fluffy pancakes with chocolate sauce','بان كيك هش بصوص الشوكولاتة','https://images.unsplash.com/photo-1567620905732-2d1ec7ab7445?auto=format&fit=crop&w=800&q=80','130','60','FOOD','MEDIUM',1),
('4','1','Classic Burger','برجر كلاسيك','Beef burger with cheddar and fries','برجر لحم بالشيدر مع بطاطس','https://images.unsplash.com/photo-1550547660-d9450f859349?auto=format&fit=crop&w=800&q=80','190','60','FOOD','MEDIUM',1),
('4','1','Veggie Pizza','بيتزا خضار','Wood-fired veggie pizza, large','بيتزا خضار على الحطب، حجم كبير','https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?auto=format&fit=crop&w=800&q=80','220','40','FOOD','LARGE',1),
('4','1','Spicy Strips','ستربس حار','Crispy spicy chicken strips meal','وجبة ستربس دجاج حار مقرمش','assets/images/products/chicken-strips.jpeg','175','60','FOOD','MEDIUM',1),
-- Food: Abou Tarek (store 1, category 1)
('1','1','Caesar Salad','سلطة سيزر','Fresh caesar salad with grilled chicken','سلطة سيزر طازجة بقطع الدجاج المشوي','https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=800&q=80','90','70','FOOD','MEDIUM',1),
-- Fashion: Zara Casual (store 2, category 2)
('2','2','Brown Casual Shoes','حذاء كاجوال بني','Genuine leather casual shoes','حذاء كاجوال جلد طبيعي','assets/images/products/casual-shoes-reference.jpeg','1450','25','CLOTHES','MEDIUM',1),
-- Fashion: Adidas Sport (store 6, category 2)
('6','2','Winter Tracksuit','تريننج شتوي','Heavy winter training suit','طقم تريننج شتوي تقيل','assets/images/products/athletic-tracksuit.jpeg','1950','30','CLOTHES','LARGE',1),
('6','2','Classic Black Cap','كاب كلاسيك أسود','Black sports cap, adjustable','كاب رياضي أسود قابل للتعديل','assets/images/products/sport-caps-gallery.jpeg','300','80','CLOTHES','MEDIUM',1),
('6','2','Red Sneakers','سنيكرز أحمر','Light running sneakers','سنيكرز خفيف للجري','https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=800&q=80','3200','20','CLOTHES','MEDIUM',1),
-- Fashion: Lacoste Luxury (store 5, category 2)
('5','2','Lacoste Sport Perfume','عطر لاكوست سبورت','Sport edition French perfume 100ml','عطر فرنسي إصدار سبورت 100 مل','assets/images/products/lacoste-perfume.jpeg','3900','15','CLOTHES','MEDIUM',1),
-- Tech: Samsung Smart Hub (store 7, category 3)
('7','3','Samsung 55 inch TV','شاشة سامسونج 55 بوصة','4K smart TV 55 inch','شاشة سمارت 4K مقاس 55 بوصة','assets/images/products/samsung-smart-hub-reference.jpeg','22000','10','TECH','LARGE',1),
('7','3','Headphones','سماعات رأس','Noise-cancelling over-ear headphones','سماعات عازلة للضوضاء','https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=800&q=80','2800','25','TECH','MEDIUM',1),
('7','3','Pro Camera','كاميرا احترافية','Mirrorless pro camera with lens','كاميرا احترافية بعدسة','https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?auto=format&fit=crop&w=800&q=80','15000','8','TECH','MEDIUM',1);
