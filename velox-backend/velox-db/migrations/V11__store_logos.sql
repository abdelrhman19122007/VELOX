-- VELOX V11 migration: store logos, cuisines and branches (run once).
USE velox_db;

ALTER TABLE stores ADD COLUMN logo VARCHAR(255) NULL;
ALTER TABLE stores ADD COLUMN cuisine VARCHAR(120) NULL;
ALTER TABLE stores ADD COLUMN branch VARCHAR(60) NULL;

UPDATE stores SET logo = NULL, cuisine = 'Oriental, Koshary, Grill', branch = 'Downtown' WHERE id = 1;
UPDATE stores SET logo = 'assets/images/stores/logo-zara.png', cuisine = 'Casual Fashion', branch = 'Zamalek' WHERE id = 2;
UPDATE stores SET logo = 'assets/images/stores/logo-apple.png', cuisine = 'Phones, Laptops', branch = 'New Cairo' WHERE id = 3;
UPDATE stores SET logo = 'assets/images/stores/logo-pizzahut.png', cuisine = 'Italian, Pizza, Burgers', branch = 'Mohandessin' WHERE id = 4;
UPDATE stores SET logo = 'assets/images/stores/logo-lacoste.png', cuisine = 'Luxury Fashion, Perfumes', branch = 'Zamalek' WHERE id = 5;
UPDATE stores SET logo = 'assets/images/stores/logo-adidas.png', cuisine = 'Sportswear, Shoes', branch = 'Maadi' WHERE id = 6;
UPDATE stores SET logo = 'assets/images/stores/logo-samsung.png', cuisine = 'Phones, TVs, Accessories', branch = 'Nasr City' WHERE id = 7;
