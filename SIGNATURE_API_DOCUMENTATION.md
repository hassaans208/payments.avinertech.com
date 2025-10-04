# Signature-Based Payment Gateway API Documentation

## Overview

The Payment Gateway API now uses a signature-based approach where all payment data is encrypted in a signature and decrypted using the external signal service at `signal.avinertech.com/api/decrypt`.

## API Endpoints

### 1. Create Payment (Signature-Based)
**POST** `/v1/payments`

Creates a new payment by decrypting the signature to extract payment data.

#### Headers
```
Content-Type: application/json
X-APP-SIGNATURE: <signature>
X-ENC-SUB: <encrypted-subdomain>
```

#### Request Body
```json
{
  "signature": "encrypted-signature-containing-payment-data"
}
```

#### Signature Data Structure
The signature contains encrypted JSON payload that will be decrypted to extract:
- `tenantId`: Tenant identifier
- `providerCode`: Payment provider code (e.g., "payoneer")
- `providerAccountId`: Provider account UUID
- `amountMinor`: Amount in minor units (e.g., cents)
- `currency`: Currency code (e.g., "USD")
- `method`: Payment method (default: "credit_card")
- `orderId`: Order identifier
- `productName`: Product name
- `serviceName`: Service name
- `companyName`: Company name
- `serviceHost`: Service host URL
- `clientDomain`: Client domain
- `clientId`: Client identifier
- `clientReferenceId`: Client reference ID
- `callbackUrl`: Callback URL for notifications
- `metadata`: Additional metadata object containing custom fields

#### Response (201 Created)
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440001",
  "tenantId": "tenant-123",
  "providerCode": "payoneer",
  "providerAccountId": "550e8400-e29b-41d4-a716-446655440000",
  "providerPaymentId": "PAY_1704067200000_ABC12345",
  "status": "INITIATED",
  "amountMinor": 29999,
  "currency": "USD",
  "method": "credit_card",
  "orderId": "ORDER-12345",
  "productName": "Premium Software License",
  "serviceName": "Enterprise Support Package",
  "companyName": "AvinerTech Solutions",
  "serviceHost": "https://example.com",
  "clientDomain": "client.example.com",
  "clientId": "client123",
  "clientReferenceId": "REF-789",
  "callbackUrl": "https://client.example.com/payment/callback",
  "checkoutUrl": "/forms/payment/checkout?paymentId=550e8400-e29b-41d4-a716-446655440001",
  "metadata": {
    "customerId": "cust_123",
    "subscriptionId": "sub_456",
    "callbackUrl": "https://client.example.com/payment/callback",
    "serviceName": "Enterprise Support Package",
    "companyName": "AvinerTech Solutions",
    "createdAt": "2024-01-01T12:00:00Z"
  },
  "createdAt": "2024-01-01T12:00:00Z",
  "updatedAt": "2024-01-01T12:00:00Z"
}
```

### 2. Get Payment
**GET** `/v1/payments/{id}`

Retrieves payment details by ID.

#### Headers
```
X-APP-SIGNATURE: <signature>
X-ENC-SUB: <encrypted-subdomain>
```

#### Response (200 OK)
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440001",
  "tenantId": "tenant-123",
  "status": "INITIATED",
  "amountMinor": 29999,
  "currency": "USD",
  "productName": "Premium Software License",
  "metadata": {
    "packageId": "pkg-123",
    "serviceId": "svc-456"
  }
}
```

### 3. Get Payment Status
**GET** `/v1/payments/{id}/status`

Gets payment status by ID.

#### Response (200 OK)
```json
{
  "paymentId": "550e8400-e29b-41d4-a716-446655440001",
  "status": "INITIATED",
  "orderId": "TEST-ORDER-12345"
}
```

### 4. Complete Payment
**POST** `/v1/payments/{id}/complete`

Completes a payment and sends callback notification.

#### Request Body
```json
{
  "cardNumber": "4111111111111111",
  "expiryDate": "12/25",
  "cvv": "123",
  "cardholderName": "John Doe",
  "billingAddress": "123 Main St",
  "city": "New York",
  "state": "NY",
  "zipCode": "10001",
  "email": "john@example.com",
  "phone": "+1234567890"
}
```

#### Response (200 OK)
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440001",
  "status": "COMPLETED",
  "amountMinor": 29999,
  "currency": "USD",
  "metadata": {
    "customerId": "cust_123",
    "subscriptionId": "sub_456",
    "completedAt": "2024-01-01T12:05:00Z",
    "paymentData": {
      "cardNumber": "4111111111111111",
      "expiryDate": "12/25",
      "cvv": "123",
      "cardholderName": "John Doe"
    }
  }
}
```

## Checkout Form Integration

### Checkout Page
**GET** `/forms/payment/checkout?paymentId={paymentId}`

Displays the checkout form with payment information loaded from the database.

The checkout page dynamically loads:
- Product name from signature data
- Service name from signature data
- Company name from signature data
- Package ID from signature data
- Service ID from signature data
- Amount and currency
- Payment ID for form submission

### Thank You Page
**GET** `/forms/payment/thankyou?paymentId={paymentId}`

Displays confirmation page after successful payment completion.

## Error Handling

All endpoints return appropriate HTTP status codes:
- `200 OK`: Successful operation
- `201 Created`: Payment created successfully
- `400 Bad Request`: Invalid request data
- `404 Not Found`: Payment not found
- `500 Internal Server Error`: Server error

Error responses include a message:
```json
{
  "error": "Error description"
}
```

## Testing

Use the provided test files:
- `test-signature-api.bat`: Windows batch script for testing
- `Signature_Payment_Gateway_API_Tests.postman_collection.json`: Postman collection

## Security Notes

- All payment data is encrypted in the signature
- Signatures are decrypted using the external signal service
- Tenant isolation is maintained through tenant ID validation
- Card details are not stored permanently (only last 4 digits logged)
- Callback URLs are validated before sending notifications
