package payments.avinertech.gateways.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import payments.avinertech.gateways.dto.PaymentCreateRequest;
import payments.avinertech.gateways.dto.PaymentResponse;
import payments.avinertech.gateways.entity.Payment;
import payments.avinertech.gateways.service.PaymentService;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for payment-related API endpoints.
 * This controller handles signature-based payment creation and management.
 */
@RestController
@RequestMapping("/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentApiController {

    private final PaymentService paymentService;

    /**
     * Creates a new payment using signature-based approach.
     * The signature contains all payment data encrypted and will be decrypted
     * to extract payment information including packageId, productName, serviceId, etc.
     * 
     * @param request The HTTP request containing tenant information
     * @param paymentRequest The payment creation request with encrypted signature
     * @return ResponseEntity containing the created payment or error
     */
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            HttpServletRequest request,
            @RequestBody PaymentCreateRequest paymentRequest) {
        
        log.info("Creating payment from signature");
        
        try {
            // Create payment by decrypting signature
            Payment payment = paymentService.createPayment(paymentRequest, request);
            
            // Convert to response DTO
            PaymentResponse response = PaymentResponse.fromPayment(payment);
            
            log.info("Successfully created payment: {} for order: {}", 
                payment.getId(), 
                payment.getOrderId());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            log.error("Failed to create payment from signature", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(PaymentResponse.error("Failed to create payment: " + e.getMessage()));
        }
    }

    /**
     * Retrieves payment details by ID.
     * 
     * @param request The HTTP request containing tenant information
     * @param id The payment UUID
     * @return ResponseEntity containing the payment or error
     */
    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPayment(
            HttpServletRequest request,
            @PathVariable UUID id) {
        
        try {
            String tenantId = (String) request.getAttribute("tenantId");
            Payment payment = paymentService.getPaymentById(id, tenantId);
            PaymentResponse response = PaymentResponse.fromPayment(payment);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to retrieve payment: {}", id, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(PaymentResponse.error("Payment not found: " + e.getMessage()));
        }
    }

    /**
     * Gets payment status by ID.
     * 
     * @param request The HTTP request containing tenant information
     * @param id The payment UUID
     * @return ResponseEntity containing the payment status
     */
    @GetMapping("/{id}/status")
    public ResponseEntity<Map<String, String>> getPaymentStatus(
            HttpServletRequest request,
            @PathVariable UUID id) {
        
        try {
            String tenantId = (String) request.getAttribute("tenantId");
            Payment payment = paymentService.getPaymentById(id, tenantId);
            
            Map<String, String> statusResponse = Map.of(
                "paymentId", payment.getId().toString(),
                "status", payment.getStatus(),
                "orderId", payment.getOrderId() != null ? payment.getOrderId() : "unknown"
            );
            
            return ResponseEntity.ok(statusResponse);
            
        } catch (Exception e) {
            log.error("Failed to retrieve payment status: {}", id, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Payment not found: " + e.getMessage()));
        }
    }

    /**
     * Completes a payment (marks as COMPLETED and sends callback).
     * This endpoint is typically called after successful payment processing.
     * 
     * @param request The HTTP request containing tenant information
     * @param id The payment UUID
     * @param paymentData Additional payment data from form submission
     * @return ResponseEntity containing the completed payment or error
     */
    @PostMapping("/{id}/complete")
    public ResponseEntity<PaymentResponse> completePayment(
            HttpServletRequest request,
            @PathVariable UUID id,
            @RequestBody Map<String, Object> paymentData) {
        
        try {
            String tenantId = (String) request.getAttribute("tenantId");
            Payment completedPayment = paymentService.completePayment(id, tenantId, paymentData);
            PaymentResponse response = PaymentResponse.fromPayment(completedPayment);
            
            log.info("Successfully completed payment: {} for order: {}", 
                completedPayment.getId(), 
                completedPayment.getOrderId());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to complete payment: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(PaymentResponse.error("Failed to complete payment: " + e.getMessage()));
        }
    }
}