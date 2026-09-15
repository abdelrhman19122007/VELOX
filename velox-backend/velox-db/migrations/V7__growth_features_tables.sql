-- VELOX V7 migration: tables/columns for the 5 growth features.
-- Run once, after V6. MariaDB-safe (inherits database default collation).
USE velox_db;

-- Feature 3: scheduled orders (NULL = ASAP, future timestamp = scheduled).
ALTER TABLE orders ADD COLUMN scheduled_for DATETIME NULL;

-- Feature 2: instant refunds (one row per order = no double compensation).
CREATE TABLE IF NOT EXISTS refunds (
  id INT AUTO_INCREMENT PRIMARY KEY,
  order_code VARCHAR(32) NOT NULL UNIQUE,
  user_id BIGINT NOT NULL,
  amount DECIMAL(10,2) NOT NULL,
  reason VARCHAR(24) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Feature 4: VELOX Plus subscriptions (one active row per user).
CREATE TABLE IF NOT EXISTS subscriptions (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  plan VARCHAR(16) NOT NULL DEFAULT 'PLUS_MONTHLY',
  started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  expires_at DATETIME NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Feature 5: price watches (one active watch per user+product).
CREATE TABLE IF NOT EXISTS price_watches (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  target_price DECIMAL(10,2) NOT NULL,
  notified TINYINT(1) NOT NULL DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_watch (user_id, product_id),
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (product_id) REFERENCES products(id)
);
