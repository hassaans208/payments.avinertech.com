-- Manual Database Setup Script
-- Run this in MariaDB/MySQL client if Flyway fails

-- Create database if it doesn't exist
CREATE DATABASE IF NOT EXISTS payments 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

-- Use the database
USE payments;

-- Create payment table
CREATE TABLE IF NOT EXISTS payment (
  id CHAR(36) PRIMARY KEY,
  tenant_id CHAR(36) NOT NULL,
  provider_code VARCHAR(255) NOT NULL,
  provider_account_id CHAR(36),
  provider_payment_id VARCHAR(255),
  status VARCHAR(50) NOT NULL,
  amount_minor BIGINT NOT NULL,
  currency CHAR(3) NOT NULL,
  method VARCHAR(50),
  order_id VARCHAR(255),
  product_name VARCHAR(255),
  service_host VARCHAR(255),
  client_domain VARCHAR(255),
  client_id VARCHAR(255),
  client_reference_id VARCHAR(255),
  metadata JSON,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_payment_tenant_provider (tenant_id, provider_code, provider_payment_id),
  UNIQUE KEY uk_payment_tenant_order (tenant_id, order_id, client_reference_id)
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idxPayment_tenant_status ON payment(tenant_id, status);
CREATE INDEX IF NOT EXISTS idxPayment_tenant_provider ON payment(tenant_id, provider_code, provider_payment_id);

-- Create Flyway schema history table
CREATE TABLE IF NOT EXISTS flyway_schema_history (
  installed_rank INT NOT NULL,
  version VARCHAR(50),
  description VARCHAR(200) NOT NULL,
  type VARCHAR(20) NOT NULL,
  script VARCHAR(1000) NOT NULL,
  checksum INT,
  installed_by VARCHAR(100) NOT NULL,
  installed_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  execution_time INT NOT NULL,
  success BOOLEAN NOT NULL,
  PRIMARY KEY (installed_rank)
);

-- Insert baseline record
INSERT IGNORE INTO flyway_schema_history 
(installed_rank, version, description, type, script, checksum, installed_by, execution_time, success)
VALUES 
(1, '0', '<< Flyway Baseline >>', 'BASELINE', '<< Flyway Baseline >>', NULL, 'manual', 0, 1);

SELECT 'Database setup completed successfully!' as status;
