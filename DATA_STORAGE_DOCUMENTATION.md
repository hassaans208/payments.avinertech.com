# Payment Gateway Data Storage Documentation

## Overview
The Payment Gateway stores comprehensive payment data in the database, including all the information from the API request and form submission.

## Database Schema

### Payment Table (`payment`)

| Column | Type | Description | Example |
|--------|------|-------------|---------|
| `id` | UUID | Primary key, auto-generated | `550e8400-e29b-41d4-a716-446655440001` |
| `tenant_id` | UUID | Tenant isolation (from authentication) | `550e8400-e29b-41d4-a716-446655440000` |
| `provider_code` | VARCHAR | Payment provider identifier | `payoneer` |
| `provider_account_id` | UUID | Provider account reference | `550e8400-e29b-41d4-a716-446655440000` |
| `provider_payment_id` | VARCHAR | Provider's payment ID | `PAY_1704067200000_ABC12345` |
| `status` | VARCHAR | Payment status | `INITIATED`, `COMPLETED`, `FAILED` |
| `amount_minor` | BIGINT | Amount in minor units (cents) | `29999` (for $299.99) |
| `currency` | VARCHAR(3) | Currency code | `USD`, `EUR`, `GBP` |
| `method` | VARCHAR | Payment method | `credit_card`, `bank_transfer` |
| `order_id` | VARCHAR | Client's order ID | `ORDER-12345` |
| `product_name` | VARCHAR | Product being purchased | `Premium Software License` |
| `service_host` | VARCHAR | Service host URL | `https://example.com` |
| `client_domain` | VARCHAR | Client's domain | `client.example.com` |
| `client_id` | VARCHAR | Client identifier | `client123` |
| `client_reference_id` | VARCHAR | Client's reference ID | `REF-789` |
| `metadata` | JSON | Additional data (JSON string) | See metadata section below |
| `created_at` | TIMESTAMP | Creation timestamp | `2024-01-01T12:00:00Z` |
| `updated_at` | TIMESTAMP | Last update timestamp | `2024-01-01T12:05:00Z` |

## Data Storage Details

### 1. API Request Data (Stored Directly)

When creating a payment via `POST /v1/payments`, the following data is stored:

```json
{
  "providerCode": "payoneer",           // → provider_code
  "providerAccountId": "550e8400...",   // → provider_account_id
  "amountMinor": 29999,                 // → amount_minor
  "currency": "USD",                    // → currency
  "method": "credit_card",              // → method
  "orderId": "ORDER-12345",             // → order_id
  "productName": "Premium Software License", // → product_name
  "serviceHost": "https://example.com",  // → service_host
  "clientDomain": "client.example.com", // → client_domain
  "clientId": "client123",              // → client_id
  "clientReferenceId": "REF-789",      // → client_reference_id
  "callbackUrl": "https://webhook.site/...", // → metadata
  "serviceName": "Enterprise Support Package", // → metadata
  "companyName": "AvinerTech Solutions", // → metadata
  "metadata": {                        // → metadata
    "customerId": "cust_123",
    "subscriptionId": "sub_456"
  }
}
```

### 2. Metadata Storage (JSON Field)

The `metadata` column stores additional data as a JSON string:

```json
{
  "callbackUrl": "https://webhook.site/your-unique-url",
  "serviceName": "Enterprise Support Package",
  "companyName": "AvinerTech Solutions",
  "data": {
    "customerId": "cust_123",
    "subscriptionId": "sub_456"
  },
  "paymentData": {
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com",
    "phone": "123-456-7890",
    "accountTitle": "Mr.",
    "cardNumber": "4111 1111 1111 1111",
    "expiryDate": "12/25",
    "cvv": "123",
    "cardholderName": "John Doe",
    "address": "123 Main Street",
    "city": "New York",
    "state": "NY",
    "zipCode": "10001",
    "country": "US"
  }
}
```

### 3. Form Submission Data (Added to Metadata)

When the checkout form is submitted, the payment data is added to the metadata:

**Before Form Submission:**
```json
{
  "callbackUrl": "https://webhook.site/your-unique-url",
  "serviceName": "Enterprise Support Package",
  "companyName": "AvinerTech Solutions",
  "data": {
    "customerId": "cust_123",
    "subscriptionId": "sub_456"
  }
}
```

**After Form Submission:**
```json
{
  "callbackUrl": "https://webhook.site/your-unique-url",
  "serviceName": "Enterprise Support Package",
  "companyName": "AvinerTech Solutions",
  "data": {
    "customerId": "cust_123",
    "subscriptionId": "sub_456"
  },
  "paymentData": {
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com",
    "phone": "123-456-7890",
    "accountTitle": "Mr.",
    "cardNumber": "4111 1111 1111 1111",
    "expiryDate": "12/25",
    "cvv": "123",
    "cardholderName": "John Doe",
    "address": "123 Main Street",
    "city": "New York",
    "state": "NY",
    "zipCode": "10001",
    "country": "US"
  }
}
```

## Data Security & Privacy

### Sensitive Data Handling

1. **Card Numbers**: Stored in metadata but should be encrypted in production
2. **CVV**: Stored temporarily, should be removed after processing
3. **Personal Information**: Stored in metadata, consider encryption
4. **Tenant Isolation**: All data is isolated by `tenant_id`

### Recommended Security Measures

```sql
-- Example: Encrypt sensitive data
UPDATE payment 
SET metadata = ENCRYPT(metadata, 'encryption_key') 
WHERE id = 'payment-id';

-- Example: Mask card numbers in logs
SELECT 
  id,
  order_id,
  JSON_EXTRACT(metadata, '$.paymentData.cardNumber') as masked_card
FROM payment 
WHERE JSON_EXTRACT(metadata, '$.paymentData.cardNumber') IS NOT NULL;
```

## Database Queries

### 1. Find Payment by Order ID
```sql
SELECT * FROM payment 
WHERE order_id = 'ORDER-12345' 
AND tenant_id = 'tenant-uuid';
```

### 2. Get Payment with Parsed Metadata
```sql
SELECT 
  id,
  status,
  amount_minor,
  currency,
  JSON_EXTRACT(metadata, '$.serviceName') as service_name,
  JSON_EXTRACT(metadata, '$.companyName') as company_name,
  JSON_EXTRACT(metadata, '$.paymentData.firstName') as customer_first_name,
  JSON_EXTRACT(metadata, '$.paymentData.email') as customer_email
FROM payment 
WHERE id = 'payment-uuid';
```

### 3. Find Payments by Status
```sql
SELECT * FROM payment 
WHERE status = 'COMPLETED' 
AND tenant_id = 'tenant-uuid'
ORDER BY created_at DESC;
```

### 4. Get Payment Statistics
```sql
SELECT 
  status,
  COUNT(*) as count,
  SUM(amount_minor) as total_amount_minor,
  AVG(amount_minor) as avg_amount_minor
FROM payment 
WHERE tenant_id = 'tenant-uuid'
GROUP BY status;
```

## Data Retention & Cleanup

### Recommended Retention Policy

1. **Completed Payments**: Keep for 7 years (compliance)
2. **Failed Payments**: Keep for 1 year
3. **Initiated Payments**: Keep for 30 days (cleanup incomplete)

### Cleanup Queries

```sql
-- Delete old incomplete payments
DELETE FROM payment 
WHERE status = 'INITIATED' 
AND created_at < NOW() - INTERVAL 30 DAY;

-- Archive old completed payments (move to archive table)
INSERT INTO payment_archive 
SELECT * FROM payment 
WHERE status = 'COMPLETED' 
AND created_at < NOW() - INTERVAL 7 YEAR;
```

## Backup & Recovery

### Backup Strategy

1. **Full Backup**: Daily at 2 AM
2. **Incremental Backup**: Every 4 hours
3. **Transaction Log Backup**: Every 15 minutes

### Recovery Procedures

```sql
-- Restore from backup
RESTORE DATABASE payments FROM backup_file;

-- Point-in-time recovery
RESTORE DATABASE payments 
FROM backup_file 
WITH STOPAT = '2024-01-01 12:00:00';
```

## Monitoring & Analytics

### Key Metrics to Track

1. **Payment Volume**: Count by day/week/month
2. **Success Rate**: Completed vs Failed payments
3. **Average Amount**: By currency and time period
4. **Processing Time**: From creation to completion

### Monitoring Queries

```sql
-- Daily payment volume
SELECT 
  DATE(created_at) as date,
  COUNT(*) as total_payments,
  COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) as completed_payments,
  SUM(CASE WHEN status = 'COMPLETED' THEN amount_minor ELSE 0 END) as total_amount
FROM payment 
WHERE created_at >= NOW() - INTERVAL 30 DAY
GROUP BY DATE(created_at)
ORDER BY date DESC;

-- Success rate by provider
SELECT 
  provider_code,
  COUNT(*) as total_payments,
  COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) as completed_payments,
  ROUND(COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) * 100.0 / COUNT(*), 2) as success_rate
FROM payment 
WHERE created_at >= NOW() - INTERVAL 7 DAY
GROUP BY provider_code;
```

## Summary

**Yes, we store ALL the data** from both the API request and form submission:

✅ **API Request Data**: All fields from PaymentCreateRequest are stored
✅ **Form Data**: All checkout form fields are stored in metadata
✅ **System Data**: Timestamps, status, tenant isolation
✅ **Provider Data**: Provider codes, account IDs, payment IDs
✅ **Client Data**: Client domains, IDs, reference IDs
✅ **Product Data**: Product names, service names, company names
✅ **Payment Data**: Amounts, currencies, methods
✅ **Customer Data**: Names, emails, addresses, card details

The system provides comprehensive data storage for audit trails, analytics, compliance, and business intelligence while maintaining tenant isolation and security.
