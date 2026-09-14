-- VELOX V2 migration (run once, in order). Safe to run on the live velox_db:
-- ENUM widens are no-ops if already applied; ADD COLUMNs fail only if
-- already present (drop/skip those lines on re-run).
-- 1) 27 governorates + SHIPPED status (matches Governorate.java / OrderStatus.java)
-- 2) order_code + loyalty counter + sessions table
-- 3) product images + Arabic names (products + stores)
USE velox_db;

-- ---------- 1) enums ----------
ALTER TABLE users MODIFY governorate ENUM('CAIRO','GIZA','ALEXANDRIA','DAKAHLIA','RED_SEA','BEHEIRA','FAYOUM','GHARBIA','ISMAILIA','MENOFIA','MINYA','QALYUBIA','NEW_VALLEY','SUEZ','ASWAN','ASYUT','BENI_SUEF','PORT_SAID','DAMIETTA','SHARKIA','SOUTH_SINAI','KAFR_EL_SHEIKH','MATROUH','LUXOR','NORTH_SINAI','QENA','SOHAG') NOT NULL;
ALTER TABLE addresses MODIFY zone ENUM('CAIRO','GIZA','ALEXANDRIA','DAKAHLIA','RED_SEA','BEHEIRA','FAYOUM','GHARBIA','ISMAILIA','MENOFIA','MINYA','QALYUBIA','NEW_VALLEY','SUEZ','ASWAN','ASYUT','BENI_SUEF','PORT_SAID','DAMIETTA','SHARKIA','SOUTH_SINAI','KAFR_EL_SHEIKH','MATROUH','LUXOR','NORTH_SINAI','QENA','SOHAG') NOT NULL;
ALTER TABLE orders MODIFY status ENUM('PENDING','PROCESSING','SHIPPED','IN_TRANSIT','ARRIVED','DELIVERED','PAID','RETURNED') NOT NULL DEFAULT 'PENDING';

-- ---------- 2) order code + loyalty + sessions ----------
ALTER TABLE orders ADD COLUMN order_code VARCHAR(20) NULL AFTER id;
UPDATE orders SET order_code = CONCAT('ORD-', id) WHERE order_code IS NULL;
ALTER TABLE orders ADD UNIQUE KEY uq_orders_code (order_code);
ALTER TABLE users ADD COLUMN loyalty_rewards_consumed INT NOT NULL DEFAULT 0;
CREATE TABLE IF NOT EXISTS sessions (
  token VARCHAR(128) NOT NULL,
  email VARCHAR(150) NOT NULL,
  expires_at DATETIME NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (token),
  KEY idx_sessions_email (email),
  KEY idx_sessions_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------- 3) images + Arabic names ----------
ALTER TABLE products ADD COLUMN image_url VARCHAR(255) NULL AFTER description;
ALTER TABLE products ADD COLUMN name_ar VARCHAR(150) NULL AFTER name;
ALTER TABLE products ADD COLUMN description_ar TEXT NULL AFTER description;
ALTER TABLE stores ADD COLUMN name_ar VARCHAR(150) NULL AFTER name;

UPDATE stores SET name_ar = 'أبو طارق والشبراوي' WHERE id = 1;
UPDATE stores SET name_ar = 'زارا كاجوال' WHERE id = 2;
UPDATE stores SET name_ar = 'آبل' WHERE id = 3;
UPDATE stores SET name_ar = 'بافالو وبيتزا هت' WHERE id = 4;
UPDATE stores SET name_ar = 'لاكوست' WHERE id = 5;
UPDATE stores SET name_ar = 'أديداس' WHERE id = 6;
UPDATE stores SET name_ar = 'سامسونج' WHERE id = 7;

UPDATE products SET name_ar = 'علبة كشري عائلية', description_ar = 'علبة كشري كبيرة تكفي العائلة.' WHERE id = 3;
UPDATE products SET name_ar = 'طقم تيشيرتات قطن أساسية', description_ar = 'تيشيرتات قطن يومية مريحة.' WHERE id = 4;
UPDATE products SET name_ar = 'آيربودز برو الجيل الثاني', description_ar = 'سماعات آبل اللاسلكية بخاصية إلغاء الضوضاء.' WHERE id = 5;
UPDATE products SET image_url = 'https://images.unsplash.com/photo-1544025162-d76694265947?auto=format&fit=crop&w=900&q=82', name_ar = 'طبق مشويات مشكل (1 كجم)', description_ar = 'تشكيلة مشويات شهية بحجم عائلي.' WHERE id = 6;
UPDATE products SET image_url = 'https://images.unsplash.com/photo-1529006557810-274b9b2fc783?auto=format&fit=crop&w=900&q=82', name_ar = 'راب شاورما لحم', description_ar = 'شاورما لحم متبلة داخل راب طازج.' WHERE id = 7;
UPDATE products SET image_url = 'assets/images/products/chicken-crepe-user.jpeg', name_ar = 'كريب تشيكن كرانشي', description_ar = 'كريب دجاج مقرمش بطعم غني.' WHERE id = 8;
UPDATE products SET image_url = 'assets/images/products/molokhia-half-chicken.jpeg', name_ar = 'ملوخية مع نصف فرخة', description_ar = 'طبق مصري تقليدي بلمسة منزلية.' WHERE id = 9;
UPDATE products SET image_url = 'https://images.unsplash.com/photo-1574071318508-1cdbab80d002?auto=format&fit=crop&w=900&q=82', name_ar = 'بيتزا سوبر سوبريم', description_ar = 'بيتزا كبيرة محملة بالإضافات.' WHERE id = 10;
UPDATE products SET image_url = 'https://images.unsplash.com/photo-1568901346375-23c9450c58cd?auto=format&fit=crop&w=900&q=82', name_ar = 'برجر لحم بالمشروم', description_ar = 'دبل برجر لحم مع مشروم وصوص خاص.' WHERE id = 11;
UPDATE products SET image_url = 'assets/images/products/chicken-strips-user.jpeg', name_ar = 'وجبة ستربس تشيكن', description_ar = 'ستربس دجاج مقرمشة مع وجبة كاملة.' WHERE id = 12;
UPDATE products SET image_url = 'https://images.unsplash.com/photo-1473093295043-cdd812d0e601?auto=format&fit=crop&w=900&q=82', name_ar = 'مكرونة ألفريدو إيطالي', description_ar = 'مكرونة كريمية بصوص ألفريدو.' WHERE id = 13;
UPDATE products SET image_url = 'assets/images/products/cheesy-garlic-bread-user.jpeg', name_ar = 'خبز بالثوم والجبنة', description_ar = 'خبز ثوم دافئ بغطاء جبنة ذائب.' WHERE id = 14;
UPDATE products SET image_url = 'assets/images/products/denim-jacket.jpeg', name_ar = 'جاكيت جينز كاجوال', description_ar = 'جاكيت جينز عملي للستايل اليومي.' WHERE id = 15;
UPDATE products SET image_url = 'assets/images/products/summer-hat-user.jpeg', name_ar = 'قبعة صيفية Bucket Hat', description_ar = 'قبعة خفيفة مناسبة لأيام الصيف.' WHERE id = 16;
UPDATE products SET image_url = 'assets/images/products/lacoste-lhomme-user.jpeg', name_ar = 'عطر لاكوست فرنسي 100 مل', description_ar = 'عطر أنيق بإطلالة فاخرة.' WHERE id = 17;
UPDATE products SET image_url = 'https://photos6.spartoo.hu/photos/160/16026859/16026859_1200_A.jpg', name_ar = 'قميص بولو كلاسيك', description_ar = 'قميص بولو بقصة كلاسيكية راقية.' WHERE id = 18;
UPDATE products SET image_url = 'assets/images/products/leather-belt-user.jpeg', name_ar = 'طقم حزام جلد طبيعي', description_ar = 'أحزمة جلد طبيعي بتشطيب فاخر.' WHERE id = 19;
UPDATE products SET image_url = 'assets/images/products/ultraboost-running.jpeg', name_ar = 'حذاء Ultraboost للجري', description_ar = 'حذاء رياضي خفيف للأداء اليومي.' WHERE id = 20;
UPDATE products SET image_url = 'assets/images/products/athletic-tracksuit-user.jpeg', name_ar = 'طقم تريننج رياضي', description_ar = 'طقم عملي للتمرين والحركة.' WHERE id = 21;
UPDATE products SET image_url = 'assets/images/products/sport-cap-wristbands.jpeg', name_ar = 'كاب رياضي + أساور', description_ar = 'إكسسوارات بسيطة تكمل لوكك الرياضي.' WHERE id = 22;
UPDATE products SET image_url = 'assets/images/products/iphone-15-pro-max.jpeg', name_ar = 'iPhone 15 Pro Max 256GB', description_ar = 'هاتف Apple الرائد بسعة 256 جيجابايت.' WHERE id = 23;
UPDATE products SET image_url = 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?auto=format&fit=crop&w=900&q=82', name_ar = 'MacBook Air M2 13 بوصة', description_ar = 'لابتوب خفيف بشريحة M2.' WHERE id = 24;
UPDATE products SET image_url = 'https://images.unsplash.com/photo-1610945415295-d9bbf067e59c?auto=format&fit=crop&w=900&q=82', name_ar = 'Samsung Galaxy S24 Ultra', description_ar = 'هاتف سامسونج الرائد للأداء القوي.' WHERE id = 25;
UPDATE products SET image_url = 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=900&q=82', name_ar = 'Galaxy Watch 6', description_ar = 'ساعة ذكية لمتابعة النشاط والإشعارات.' WHERE id = 26;
UPDATE products SET image_url = 'assets/images/products/wireless-charger-user.jpeg', name_ar = 'قاعدة شحن لاسلكية سريعة', description_ar = 'شحن لاسلكي سريع وعملي للمكتب والمنزل.' WHERE id = 27;
