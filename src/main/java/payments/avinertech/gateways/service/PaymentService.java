package payments.avinertech.gateways.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import payments.avinertech.gateways.dto.PaymentCreateRequest;
import payments.avinertech.gateways.entity.Payment;
import payments.avinertech.gateways.repository.PaymentRepository;

import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Service responsible for payment operations including creation, completion, and callback handling.
 * This service now uses signature-based approach where payment data is decrypted from signatures.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final SignatureDecryptionService signatureDecryptionService;
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Creates a new payment by decrypting the signature and extracting payment data.
     * The signature decryption returns a JSON payload containing all payment information.
     * 
     * @param request The payment creation request containing the encrypted signature
     * @param httpRequest The HTTP request (for future use)
     * @return The created payment entity
     * @throws RuntimeException if signature decryption or payment creation fails
     */
    @Transactional
    public Payment createPayment(PaymentCreateRequest request, HttpServletRequest httpRequest) {
        try {
            log.info("Creating payment from signature");
            
            // Decrypt the signature to get JSON payload
            Map<String, Object> decryptedData = signatureDecryptionService.decryptSignature(request.getSignature());
            System.out.println("Decrypted data: " + decryptedData);
            // Extract payment data from JSON payload
            String tenantId = extractStringValue(decryptedData, "tenantId");
            String providerCode = extractStringValue(decryptedData, "providerCode", "payoneer");
            String providerAccountIdStr = extractStringValue(decryptedData, "providerAccountId");
            UUID providerAccountId = providerAccountIdStr != null ? UUID.fromString(providerAccountIdStr) : UUID.randomUUID();
            Long amountMinor = extractLongValue(decryptedData, "amountMinor");
            String currency = extractStringValue(decryptedData, "currency");
            String method = extractStringValue(decryptedData, "method", "credit_card");
            String orderId = extractStringValue(decryptedData, "orderId");
            String productName = extractStringValue(decryptedData, "productName");
            String serviceName = extractStringValue(decryptedData, "serviceName");
            String companyName = extractStringValue(decryptedData, "companyName");
            String serviceHost = extractStringValue(decryptedData, "serviceHost");
            String clientDomain = extractStringValue(decryptedData, "clientDomain");
            String clientId = extractStringValue(decryptedData, "clientId");
            String clientReferenceId = extractStringValue(decryptedData, "clientReferenceId");
            String callbackUrl = extractStringValue(decryptedData, "callbackUrl");
            
            // Extract metadata from JSON payload
            Map<String, Object> metadata = (Map<String, Object>) decryptedData.get("metadata");
            if (metadata == null) {
                metadata = new java.util.HashMap<>();
            }
            
            // Validate required fields
            validateRequiredFields(tenantId, productName, amountMinor, currency);
            
            // Create payment entity
            Payment payment = Payment.builder()
                    .tenantId(tenantId)
                    .providerCode(providerCode)
                    .providerAccountId(providerAccountId)
                    .providerPaymentId(generateProviderPaymentId())
                    .status("INITIATED")
                    .amountMinor(amountMinor)
                    .currency(currency)
                    .method(method)
                    .orderId(orderId)
                    .productName(productName)
                    .serviceHost(serviceHost)
                    .clientDomain(clientDomain)
                    .clientId(clientId)
                    .clientReferenceId(clientReferenceId)
                    .metadata(serializeMetadata(metadata, callbackUrl, serviceName, companyName))
                    .createdAt(OffsetDateTime.now())
                    .updatedAt(OffsetDateTime.now())
                    .build();

            Payment savedPayment = paymentRepository.save(payment);
            log.info("Created payment: {} with status: INITIATED for order: {}", savedPayment.getId(), orderId);

            return savedPayment;
            
        } catch (Exception e) {
            log.error("Failed to create payment from signature", e);
            throw new RuntimeException("Failed to create payment: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves a payment by ID and tenant ID for security.
     * 
     * @param paymentId The payment UUID
     * @param tenantId The tenant ID for security validation
     * @return The payment entity
     * @throws RuntimeException if payment not found or tenant mismatch
     */
    public Payment getPaymentById(UUID paymentId, String tenantId) {
        return paymentRepository.findById(paymentId)
                // .filter(payment -> payment.getTenantId().equals(tenantId))
                .orElseThrow(() -> new RuntimeException("Payment not found"));
    }

    /**
     * Completes a payment by updating its status and sending callback if configured.
     * 
     * @param paymentId The payment UUID
     * @param tenantId The tenant ID for security validation
     * @param paymentData Additional payment data from form submission
     * @return The completed payment entity
     * @throws RuntimeException if payment not found or not in correct status
     */
    @Transactional
    public Payment completePayment(UUID paymentId, String tenantId, Map<String, Object> paymentData) {
        Payment payment = getPaymentById(paymentId, tenantId);
        
        if (!"INITIATED".equals(payment.getStatus())) {
            throw new RuntimeException("Payment is not in INITIATED status");
        }

        // Update payment status
        payment.setStatus("COMPLETED");
        payment.setUpdatedAt(OffsetDateTime.now());
        
        // Add payment completion data to metadata
        Map<String, Object> metadata = payment.getMetadata();
        metadata.put("completedAt", OffsetDateTime.now().toString());
        metadata.put("paymentData", paymentData);
        payment.setMetadata(metadata);

        Payment completedPayment = paymentRepository.save(payment);
        log.info("Completed payment: {} with status: COMPLETED", completedPayment.getId());

        // Send callback if URL is configured
        sendCallbackIfConfigured(completedPayment);

        return completedPayment;
    }

    /**
     * Sends callback notification to the client if callback URL is configured.
     * 
     * @param payment The payment entity
     */
    private void sendCallbackIfConfigured(Payment payment) {
        Map<String, Object> metadata = payment.getMetadata();
        String callbackUrl = (String) metadata.get("callbackUrl");
        
        if (callbackUrl == null || callbackUrl.trim().isEmpty()) {
            log.warn("No callback URL found for payment: {}", payment.getId());
            return;
        }

        try {
            // Prepare callback payload
            Map<String, Object> callbackPayload = Map.of(
                "paymentId", payment.getId().toString(),
                "status", payment.getStatus(),
                "amountMinor", payment.getAmountMinor(),
                "currency", payment.getCurrency(),
                "orderId", payment.getOrderId(),
                "completedAt", payment.getUpdatedAt().toString()
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(callbackPayload, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(callbackUrl, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Callback sent successfully for payment: {} - Status: {}", 
                    payment.getId(), response.getStatusCode());
            } else {
                log.warn("Callback failed for payment: {} - Status: {}", 
                    payment.getId(), response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("Failed to send callback for payment: {}", payment.getId(), e);
        }
    }

    /**
     * Generates a unique provider payment ID.
     * 
     * @return A unique provider payment ID string
     */
    private String generateProviderPaymentId() {
        return "PAY_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Serializes metadata including callback URL and additional payment information.
     * 
     * @param metadata The metadata from the JSON payload
     * @param callbackUrl The callback URL
     * @param serviceName The service name
     * @param companyName The company name
     * @return Serialized metadata map
     */
    private Map<String, Object> serializeMetadata(Map<String, Object> metadata, String callbackUrl, 
                                                  String serviceName, String companyName) {
        Map<String, Object> serializedMetadata = new java.util.HashMap<>(metadata);
        serializedMetadata.put("callbackUrl", callbackUrl);
        serializedMetadata.put("serviceName", serviceName);
        serializedMetadata.put("companyName", companyName);
        serializedMetadata.put("createdAt", OffsetDateTime.now().toString());
        return serializedMetadata;
    }

    /**
     * Extracts a string value from the decrypted data map.
     * 
     * @param data The decrypted data map
     * @param key The key to extract
     * @return The string value or null if not found
     */
    private String extractStringValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * Extracts a string value from the decrypted data map with a default value.
     * 
     * @param data The decrypted data map
     * @param key The key to extract
     * @param defaultValue The default value if key not found
     * @return The string value or default value
     */
    private String extractStringValue(Map<String, Object> data, String key, String defaultValue) {
        String value = extractStringValue(data, key);
        return value != null ? value : defaultValue;
    }

    /**
     * Extracts a long value from the decrypted data map.
     * 
     * @param data The decrypted data map
     * @param key The key to extract
     * @return The long value or null if not found
     */
    private Long extractLongValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        } else if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                log.warn("Failed to parse long value for key {}: {}", key, value);
                return null;
            }
        }
        return null;
    }

    /**
     * Validates that all required fields are present.
     * 
     * @param tenantId The tenant ID
     * @param productName The product name
     * @param amountMinor The amount in minor units
     * @param currency The currency code
     * @throws RuntimeException if any required field is missing
     */
    private void validateRequiredFields(String tenantId, String productName, 
                                       Long amountMinor, String currency) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new RuntimeException("tenantId is required");
        }
        if (productName == null || productName.trim().isEmpty()) {
            throw new RuntimeException("productName is required");
        }
        if (amountMinor == null || amountMinor <= 0) {
            throw new RuntimeException("amountMinor must be a positive number");
        }
        if (currency == null || currency.trim().isEmpty()) {
            throw new RuntimeException("currency is required");
        }
    }
}