# Payment Gateway API Documentation

## Overview
This payment gateway provides a complete payment processing system with iframe integration and callback functionality for third-party services.

## API Endpoints

### 1. Create Payment
**POST** `/v1/payments`

Creates a new payment with `INITIATED` status. This endpoint is called by third-party services to initiate a payment.

#### Request Headers
```
Content-Type: application/json
X-APP-SIGNATURE: <signature>
X-ENC-SUB: <encrypted-subdomain>
```

#### Request Body
```json
{
  "tenantId": "tenant-123",
  "providerCode": "payoneer",
  "providerAccountId": "550e8400-e29b-41d4-a716-446655440000",
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
  "metadata": {
    "customerId": "cust_123",
    "subscriptionId": "sub_456"
  }
}
```

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
    "subscriptionId": "sub_456"
  },
  "createdAt": "2024-01-01T12:00:00Z",
  "updatedAt": "2024-01-01T12:00:00Z"
}
```

### 2. Get Payment
**GET** `/v1/payments/{id}`

Retrieves payment details by ID.

#### Response (200 OK)
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440001",
  "status": "INITIATED",
  "amountMinor": 29999,
  "currency": "USD",
  "orderId": "ORDER-12345",
  "createdAt": "2024-01-01T12:00:00Z",
  "updatedAt": "2024-01-01T12:00:00Z"
}
```

### 3. Complete Payment
**POST** `/v1/payments/{id}/complete`

Updates payment status to `COMPLETED` and triggers callback to third party.

#### Request Body
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "phone": "123-456-7890",
  "cardNumber": "1234 5678 9012 3456",
  "expiryDate": "12/25",
  "cvv": "123",
  "cardholderName": "John Doe",
  "address": "123 Main St",
  "city": "New York",
  "state": "NY",
  "zipCode": "10001",
  "country": "US"
}
```

#### Response (200 OK)
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440001",
  "status": "COMPLETED",
  "amountMinor": 29999,
  "currency": "USD",
  "orderId": "ORDER-12345",
  "updatedAt": "2024-01-01T12:05:00Z"
}
```

### 4. Get Payment Status
**GET** `/v1/payments/{id}/status`

Quick status check for payment.

#### Response (200 OK)
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440001",
  "status": "COMPLETED",
  "amount": 29999,
  "currency": "USD",
  "createdAt": "2024-01-01T12:00:00Z",
  "updatedAt": "2024-01-01T12:05:00Z"
}
```

## Checkout Form Integration

### Iframe Integration
The checkout form can be embedded in an iframe using the `checkoutUrl` from the payment creation response:

```html
<iframe 
  src="https://gateway.example.com/forms/payment/checkout?paymentId=550e8400-e29b-41d4-a716-446655440001"
  width="100%" 
  height="800px"
  frameborder="0">
</iframe>
```

### Direct Link Integration
Alternatively, redirect users directly to the checkout page:

```javascript
// After creating payment
const payment = await createPayment(paymentData);
window.location.href = payment.checkoutUrl;
```

## Callback System

### Callback URL
When a payment is completed, the system sends a POST request to the `callbackUrl` provided during payment creation.

### Callback Request
```json
{
  "paymentId": "550e8400-e29b-41d4-a716-446655440001",
  "status": "COMPLETED",
  "amount": 29999,
  "currency": "USD",
  "orderId": "ORDER-12345",
  "clientReferenceId": "REF-789",
  "timestamp": "2024-01-01T12:05:00Z"
}
```

### Callback Headers
```
Content-Type: application/json
X-Payment-Signature: sig_550e8400
```

### Callback Response
Your callback endpoint should respond with HTTP 200 OK:

```json
{
  "status": "received",
  "message": "Payment callback processed successfully"
}
```

## Payment Statuses

- `INITIATED`: Payment created, waiting for user input
- `COMPLETED`: Payment successfully processed
- `FAILED`: Payment failed (future implementation)
- `CANCELLED`: Payment cancelled by user (future implementation)

## Error Handling

### Error Response Format
```json
{
  "error": "Error message description"
}
```

### Common Error Codes
- `400 Bad Request`: Invalid request data
- `403 Forbidden`: Missing or invalid authentication
- `404 Not Found`: Payment not found
- `500 Internal Server Error`: Server error

## Security Features

- **Tenant Isolation**: All payments are isolated by tenant ID
- **Signature Verification**: All API requests require valid signatures
- **Callback Signatures**: Callbacks include verification signatures
- **PCI Compliance**: Card data is handled securely
- **HTTPS Only**: All communications encrypted

## Integration Examples

### JavaScript Integration
```javascript
// Create payment
const createPayment = async (paymentData) => {
  const response = await fetch('/v1/payments', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-APP-SIGNATURE': 'your-signature',
      'X-ENC-SUB': 'your-encrypted-subdomain'
    },
    body: JSON.stringify(paymentData)
  });
  return response.json();
};

// Open checkout in iframe
const openCheckout = (paymentId) => {
  const iframe = document.createElement('iframe');
  iframe.src = `/forms/payment/checkout?paymentId=${paymentId}`;
  iframe.width = '100%';
  iframe.height = '800px';
  document.body.appendChild(iframe);
};
```

### PHP Integration
```php
// Create payment
$paymentData = [
    'providerCode' => 'payoneer',
    'amountMinor' => 29999,
    'currency' => 'USD',
    'orderId' => 'ORDER-12345',
    'callbackUrl' => 'https://yoursite.com/callback'
];

$response = curl_exec(curl_init([
    CURLOPT_URL => 'https://gateway.example.com/v1/payments',
    CURLOPT_POST => true,
    CURLOPT_POSTFIELDS => json_encode($paymentData),
    CURLOPT_HTTPHEADER => [
        'Content-Type: application/json',
        'X-APP-SIGNATURE: your-signature',
        'X-ENC-SUB: your-encrypted-subdomain'
    ]
]));

$payment = json_decode($response, true);
$checkoutUrl = $payment['checkoutUrl'];
```

## Testing

### Test Payment Creation
```bash
curl -X POST https://gateway.example.com/v1/payments \
  -H "Content-Type: application/json" \
  -H "X-APP-SIGNATURE: test-signature" \
  -H "X-ENC-SUB: test-subdomain" \
  -d '{
    "providerCode": "payoneer",
    "amountMinor": 1000,
    "currency": "USD",
    "orderId": "TEST-123",
    "callbackUrl": "https://webhook.site/your-url"
  }'
```

### Test Payment Status
```bash
curl -X GET https://gateway.example.com/v1/payments/{payment-id}/status \
  -H "X-APP-SIGNATURE: test-signature" \
  -H "X-ENC-SUB: test-subdomain"
```
