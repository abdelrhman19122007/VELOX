-- VELOX V3 migration (run once, after V2). Delivery simulation + nullable
-- order links for app-level feedback, plus the system courier account.
USE velox_db;

ALTER TABLE deliveries MODIFY zone ENUM('CAIRO','GIZA','ALEXANDRIA','DAKAHLIA','RED_SEA','BEHEIRA','FAYOUM','GHARBIA','ISMAILIA','MENOFIA','MINYA','QALYUBIA','NEW_VALLEY','SUEZ','ASWAN','ASYUT','BENI_SUEF','PORT_SAID','DAMIETTA','SHARKIA','SOUTH_SINAI','KAFR_EL_SHEIKH','MATROUH','LUXOR','NORTH_SINAI','QENA','SOHAG') NOT NULL;
ALTER TABLE complaints MODIFY order_id BIGINT NULL;
ALTER TABLE reviews MODIFY order_id BIGINT NULL;

-- System courier used by the delivery simulator (visible, documented).
INSERT INTO users (full_name, email, password_hash, phone_number, governorate, role)
SELECT 'VELOX Courier (system)', 'courier@velox.local', '*', '01000000000', 'CAIRO', 'DRIVER'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'courier@velox.local');
