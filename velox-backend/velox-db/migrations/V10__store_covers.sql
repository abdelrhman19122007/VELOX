-- VELOX V10 migration: store cover photos (run once).
USE velox_db;

ALTER TABLE stores ADD COLUMN cover_image VARCHAR(255) NULL;

UPDATE stores SET cover_image = 'assets/images/products/koshary-large.jpg' WHERE id = 1;
UPDATE stores SET cover_image = 'assets/images/stores/store2-zara.jpg' WHERE id = 2;
UPDATE stores SET cover_image = 'assets/images/stores/store3-apple.jpg' WHERE id = 3;
UPDATE stores SET cover_image = 'assets/images/stores/store4-buffalo.jpg' WHERE id = 4;
UPDATE stores SET cover_image = 'assets/images/stores/store5-lacoste.jpg' WHERE id = 5;
UPDATE stores SET cover_image = 'assets/images/stores/store6-adidas.jpg' WHERE id = 6;
UPDATE stores SET cover_image = 'assets/images/stores/store7-samsung.jpg' WHERE id = 7;
