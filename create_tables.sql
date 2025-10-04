-- Simple MariaDB-compatible schema for payment gateway
-- Run this manually in your MariaDB database

USE payments;

-- Drop tables if they exist (in correct order due to foreign keys)
DROP TABLE IF EXISTS webhook_delivery_attempt;
DROP TABLE IF EXISTS webhook_event;
DROP TABLE IF EXISTS ledger_entry;
DROP TABLE IF EXISTS journal_entry;
DROP TABLE IF EXISTS ledger_account;
DROP TABLE IF EXISTS recon_item;
DROP TABLE IF EXISTS recon_run;
DROP TABLE IF EXISTS refund;
DROP TABLE IF EXISTS payout;
DROP TABLE IF EXISTS payment_method;
DROP TABLE IF EXISTS payment;
DROP TABLE IF EXISTS idempotency_key;

-- Create tables
CREATE TABLE payment (
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
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE payment_method (
  id CHAR(36) PRIMARY KEY,
  tenant_id CHAR(36) NOT NULL,
  payment_id CHAR(36) NOT NULL,
  token VARCHAR(255),
  brand VARCHAR(50),
  last4 VARCHAR(4),
  exp_month SMALLINT,
  exp_year SMALLINT,
  details JSON,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_payment_method_payment (payment_id)
);

CREATE TABLE refund (
  id CHAR(36) PRIMARY KEY,
  tenant_id CHAR(36) NOT NULL,
  payment_id CHAR(36) NOT NULL,
  provider_code VARCHAR(255) NOT NULL,
  provider_refund_id VARCHAR(255),
  status VARCHAR(50) NOT NULL,
  amount_minor BIGINT NOT NULL,
  currency CHAR(3) NOT NULL,
  reason VARCHAR(255),
  metadata JSON,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE payout (
  id CHAR(36) PRIMARY KEY,
  tenant_id CHAR(36) NOT NULL,
  payee_label VARCHAR(255),
  provider_code VARCHAR(255) NOT NULL,
  provider_account_id CHAR(36),
  provider_payout_id VARCHAR(255),
  status VARCHAR(50) NOT NULL,
  batch_id VARCHAR(255),
  amount_minor BIGINT NOT NULL,
  currency CHAR(3) NOT NULL,
  description VARCHAR(255),
  metadata JSON,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE idempotency_key (
  `key` VARCHAR(255) PRIMARY KEY,
  tenant_id CHAR(36) NOT NULL,
  scope VARCHAR(255) NOT NULL,
  request_hash VARCHAR(255) NOT NULL,
  response_json JSON,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  ttl_seconds INT NOT NULL DEFAULT 86400
);

CREATE TABLE webhook_event (
  id CHAR(36) PRIMARY KEY,
  tenant_id CHAR(36) NOT NULL,
  provider_code VARCHAR(255) NOT NULL,
  provider_event_id VARCHAR(255) NOT NULL,
  type VARCHAR(255) NOT NULL,
  signature TEXT,
  payload_json JSON NOT NULL,
  received_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  processed_at TIMESTAMP,
  status VARCHAR(50) NOT NULL DEFAULT 'RECEIVED',
  dedupe_key VARCHAR(255) NOT NULL,
  UNIQUE KEY uk_webhook_tenant_provider (tenant_id, provider_code, provider_event_id),
  UNIQUE KEY uk_webhook_tenant_dedupe (tenant_id, dedupe_key)
);

CREATE TABLE webhook_delivery_attempt (
  id CHAR(36) PRIMARY KEY,
  tenant_id CHAR(36) NOT NULL,
  target_url VARCHAR(500) NOT NULL,
  event_id CHAR(36) NOT NULL,
  status INT NOT NULL,
  response_text TEXT,
  attempted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ledger_account (
  id CHAR(36) PRIMARY KEY,
  tenant_id CHAR(36) NOT NULL,
  code VARCHAR(255) NOT NULL,
  name VARCHAR(255) NOT NULL,
  currency CHAR(3) NOT NULL,
  UNIQUE KEY uk_ledger_account_tenant_code (tenant_id, code, currency)
);

CREATE TABLE journal_entry (
  id CHAR(36) PRIMARY KEY,
  tenant_id CHAR(36) NOT NULL,
  ref_type VARCHAR(255) NOT NULL,
  ref_id CHAR(36) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ledger_entry (
  id CHAR(36) PRIMARY KEY,
  tenant_id CHAR(36) NOT NULL,
  journal_id CHAR(36) NOT NULL,
  account_id CHAR(36) NOT NULL,
  amount_minor BIGINT NOT NULL,
  direction VARCHAR(10) NOT NULL,
  currency CHAR(3) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE recon_run (
  id CHAR(36) PRIMARY KEY,
  tenant_id CHAR(36) NOT NULL,
  provider_code VARCHAR(255) NOT NULL,
  period_start DATE NOT NULL,
  period_end DATE NOT NULL,
  status VARCHAR(50) NOT NULL DEFAULT 'RUNNING',
  diff_count INT NOT NULL DEFAULT 0,
  notes TEXT,
  started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  finished_at TIMESTAMP
);

CREATE TABLE recon_item (
  id CHAR(36) PRIMARY KEY,
  tenant_id CHAR(36) NOT NULL,
  recon_run_id CHAR(36) NOT NULL,
  kind VARCHAR(50) NOT NULL,
  our_ref CHAR(36),
  provider_ref VARCHAR(255),
  amount_minor BIGINT NOT NULL,
  currency CHAR(3) NOT NULL,
  occurred_at TIMESTAMP NOT NULL,
  match_status VARCHAR(50) NOT NULL,
  details JSON
);

-- Add foreign key constraints
ALTER TABLE payment_method ADD CONSTRAINT fk_payment_method_payment 
    FOREIGN KEY (payment_id) REFERENCES payment(id) ON DELETE CASCADE;

ALTER TABLE refund ADD CONSTRAINT fk_refund_payment 
    FOREIGN KEY (payment_id) REFERENCES payment(id);

ALTER TABLE webhook_delivery_attempt ADD CONSTRAINT fk_webhook_delivery_event 
    FOREIGN KEY (event_id) REFERENCES webhook_event(id) ON DELETE CASCADE;

ALTER TABLE ledger_entry ADD CONSTRAINT fk_ledger_journal 
    FOREIGN KEY (journal_id) REFERENCES journal_entry(id) ON DELETE CASCADE;

ALTER TABLE ledger_entry ADD CONSTRAINT fk_ledger_account 
    FOREIGN KEY (account_id) REFERENCES ledger_account(id);

ALTER TABLE recon_item ADD CONSTRAINT fk_recon_item_run 
    FOREIGN KEY (recon_run_id) REFERENCES recon_run(id) ON DELETE CASCADE;

-- Create indexes
CREATE INDEX idx_payment_tenant_status ON payment(tenant_id, status);
CREATE INDEX idx_payment_tenant_provider ON payment(tenant_id, provider_code, provider_payment_id);
CREATE INDEX idx_refund_tenant_payment ON refund(tenant_id, payment_id);
CREATE INDEX idx_payout_tenant_status ON payout(tenant_id, status);
CREATE INDEX idx_webhook_dedupe ON webhook_event(tenant_id, dedupe_key);
CREATE INDEX idx_journal_created ON journal_entry(tenant_id, created_at);
CREATE INDEX idx_ledger_account ON ledger_entry(tenant_id, account_id);
