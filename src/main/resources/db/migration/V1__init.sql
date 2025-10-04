-- MySQL Money & ID conventions
-- * Use CHAR(36) for UUID storage
-- * All monetary amounts are in MINOR units (cents, paisa, etc.) as BIGINT
-- * ISO 4217 currency codes (CHAR(3))

-- Payments (incoming)
CREATE TABLE payment (
  id CHAR(36) PRIMARY KEY,
  tenant_id CHAR(36) NOT NULL,
  provider_code VARCHAR(255) NOT NULL,          -- 'stripe','adyen','paypal','payoneer', ...
  provider_account_id CHAR(36),                 -- optional: your stored ref to provider account
  provider_payment_id VARCHAR(255),               -- external id at provider
  status VARCHAR(50) NOT NULL,                   -- PENDING|REQUIRES_ACTION|SUCCEEDED|FAILED|CANCELED|INITIATED|COMPLETED
  amount_minor BIGINT NOT NULL,
  currency CHAR(3) NOT NULL,
  method VARCHAR(50),                             -- CARD|WALLET|BANK_TRANSFER|...
  order_id VARCHAR(255),
  product_name VARCHAR(255),                      -- snapshot for reporting (no separate product table needed)
  service_host VARCHAR(255),                      -- snapshot: host/service name from request context if desired
  client_domain VARCHAR(255),                     -- snapshot from verify API response
  client_id VARCHAR(255),                         -- snapshot from verify API response
  client_reference_id VARCHAR(255),               -- idempotent ref from client
  metadata JSON,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_payment_tenant_provider (tenant_id, provider_code, provider_payment_id),
  UNIQUE KEY uk_payment_tenant_order (tenant_id, order_id, client_reference_id)
);

-- Payment method (non-sensitive tokenized details)
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
  UNIQUE KEY uk_payment_method_payment (payment_id),
  CONSTRAINT fk_payment_method_payment 
    FOREIGN KEY (payment_id) REFERENCES payment(id) ON DELETE CASCADE
);

-- Refunds (incoming negative)
CREATE TABLE refund (
  id CHAR(36) PRIMARY KEY,
  tenant_id CHAR(36) NOT NULL,
  payment_id CHAR(36) NOT NULL,
  provider_code VARCHAR(255) NOT NULL,
  provider_refund_id VARCHAR(255),
  status VARCHAR(50) NOT NULL,                    -- PENDING|SUCCEEDED|FAILED|CANCELED
  amount_minor BIGINT NOT NULL,
  currency CHAR(3) NOT NULL,
  reason VARCHAR(255),
  metadata JSON,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_refund_tenant_provider (tenant_id, provider_code, provider_refund_id),
  CONSTRAINT fk_refund_payment 
    FOREIGN KEY (payment_id) REFERENCES payment(id)
);

-- Payouts (outgoing)
CREATE TABLE payout (
  id CHAR(36) PRIMARY KEY,
  tenant_id CHAR(36) NOT NULL,
  payee_label VARCHAR(255),                      -- free text label or external id; no payee table required
  provider_code VARCHAR(255) NOT NULL,
  provider_account_id CHAR(36),
  provider_payout_id VARCHAR(255),
  status VARCHAR(50) NOT NULL,                     -- PENDING|PROCESSING|SUCCEEDED|FAILED|RETURNED
  batch_id VARCHAR(255),
  amount_minor BIGINT NOT NULL,
  currency CHAR(3) NOT NULL,
  description VARCHAR(255),
  metadata JSON,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_payout_tenant_provider (tenant_id, provider_code, provider_payout_id)
);

-- Idempotency
CREATE TABLE idempotency_key (
  `key` VARCHAR(255) PRIMARY KEY,
  tenant_id CHAR(36) NOT NULL,
  scope VARCHAR(255) NOT NULL,                   -- e.g., POST /v1/payments
  request_hash VARCHAR(255) NOT NULL,
  response_json JSON,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  ttl_seconds INT NOT NULL DEFAULT 86400
);

-- Webhook ingress (provider -> us)
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
  status VARCHAR(50) NOT NULL DEFAULT 'RECEIVED', -- RECEIVED|PROCESSED|SKIPPED|FAILED
  dedupe_key VARCHAR(255) NOT NULL,
  UNIQUE KEY uk_webhook_tenant_provider (tenant_id, provider_code, provider_event_id),
  UNIQUE KEY uk_webhook_tenant_dedupe (tenant_id, dedupe_key)
);

-- Optional: outbound webhooks to clients (if you notify them)
CREATE TABLE webhook_delivery_attempt (
  id CHAR(36) PRIMARY KEY,
  tenant_id CHAR(36) NOT NULL,
  target_url VARCHAR(500) NOT NULL,
  event_id CHAR(36) NOT NULL,
  status INT NOT NULL,                             -- HTTP status or -1
  response_text TEXT,
  attempted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_webhook_delivery_event 
    FOREIGN KEY (event_id) REFERENCES webhook_event(id) ON DELETE CASCADE
);

-- Accounting (double-entry)
CREATE TABLE ledger_account (
  id CHAR(36) PRIMARY KEY,
  tenant_id CHAR(36) NOT NULL,
  code VARCHAR(255) NOT NULL,                    -- CASH_PROVIDER, PAYMENTS_RECEIVABLE, REFUNDS_PAYABLE, PAYOUTS_PAYABLE
  name VARCHAR(255) NOT NULL,
  currency CHAR(3) NOT NULL,
  UNIQUE KEY uk_ledger_account_tenant_code (tenant_id, code, currency)
);

CREATE TABLE journal_entry (
  id CHAR(36) PRIMARY KEY,
  tenant_id CHAR(36) NOT NULL,
  ref_type VARCHAR(255) NOT NULL,                -- PAYMENT|REFUND|PAYOUT|ADJUSTMENT
  ref_id CHAR(36) NOT NULL,                      -- id of payment/refund/payout
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ledger_entry (
  id CHAR(36) PRIMARY KEY,
  tenant_id CHAR(36) NOT NULL,
  journal_id CHAR(36) NOT NULL,
  account_id CHAR(36) NOT NULL,
  amount_minor BIGINT NOT NULL,
  direction VARCHAR(10) NOT NULL,                -- DEBIT|CREDIT
  currency CHAR(3) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_ledger_journal 
    FOREIGN KEY (journal_id) REFERENCES journal_entry(id) ON DELETE CASCADE,
  CONSTRAINT fk_ledger_account 
    FOREIGN KEY (account_id) REFERENCES ledger_account(id)
);

-- Reconciliation
CREATE TABLE recon_run (
  id CHAR(36) PRIMARY KEY,
  tenant_id CHAR(36) NOT NULL,
  provider_code VARCHAR(255) NOT NULL,
  period_start DATE NOT NULL,
  period_end DATE NOT NULL,
  status VARCHAR(50) NOT NULL DEFAULT 'RUNNING', -- RUNITING|DONE|FAILED
  diff_count INT NOT NULL DEFAULT 0,
  notes TEXT,
  started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  finished_at TIMESTAMP
);

CREATE TABLE recon_item (
  id CHAR(36) PRIMARY KEY,
  tenant_id CHAR(36) NOT NULL,
  recon_run_id CHAR(36) NOT NULL,
  kind VARCHAR(50) NOT NULL,                     -- PAYMENT|REFUND|PAYOUT|FEE
  our_ref CHAR(36),
  provider_ref VARCHAR(255),
  amount_minor BIGINT NOT NULL,
  currency CHAR(3) NOT NULL,
  occurred_at TIMESTAMP NOT NULL,
  match_status VARCHAR(50) NOT NULL,             -- MATCHED|MISSING_LOCAL|MISSING_PROVIDER|AMOUNT_MISMATCH
  details JSON,
  CONSTRAINT fk_recon_item_run 
    FOREIGN KEY (recon_run_id) REFERENCES recon_run(id) ON DELETE CASCADE
);

-- Indexes (common)
CREATE INDEX idxPayment_tenant_status ON payment(tenant_id, status);
CREATE INDEX idxPayment_tenant_provider ON payment(tenant_id, provider_code, provider_payment_id);
CREATE INDEX idx_refund_tenant_payment ON refund(tenant_id, payment_id);
CREATE INDEX idx_payout_tenant_status ON payout(tenant_id, status);
CREATE INDEX idx_webhook_dedupe ON webhook_event(tenant_id, dedupe_key);
CREATE INDEX idx_journal_created ON journal_entry(tenant_id, created_at);
CREATE INDEX idx_ledger_account ON ledger_entry(tenant_id, account_id);