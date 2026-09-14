-- VELOX V6 migration: payment method columns, saved cards provider, OTP codes.
USE velox_db;

ALTER TABLE user_payment_cards ADD COLUMN method_type VARCHAR(10) NOT NULL DEFAULT 'CARD' AFTER user_id;
ALTER TABLE user_payment_cards ADD COLUMN provider VARCHAR(30) NULL AFTER card_brand;

CREATE TABLE IF NOT EXISTS otp_codes (
  id BIGINT NOT NULL AUTO_INCREMENT,
  email VARCHAR(150) NOT NULL,
  code_hash VARCHAR(255) NOT NULL,
  expires_at DATETIME NOT NULL,
  attempts INT NOT NULL DEFAULT 0,
  used TINYINT(1) NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_otp_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
