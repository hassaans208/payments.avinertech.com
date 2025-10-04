package payments.avinertech.gateways.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import payments.avinertech.gateways.entity.Payment;
import payments.avinertech.gateways.service.PaymentService;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for handling checkout form pages.
 * This controller retrieves payment data from the database using payment ID
 * and displays the checkout form with dynamic payment information.
 */
@Controller
@RequestMapping("/forms")
@RequiredArgsConstructor
@Slf4j
public class CheckoutController {

    private final PaymentService paymentService;

    /**
     * Displays the checkout page with payment information loaded from database.
     * The payment ID comes from the signature and is used to retrieve payment details.
     * 
     * @param paymentId The payment UUID from the signature
     * @param model The Spring MVC model for passing data to the view
     * @param request The HTTP request
     * @return The checkout page template or error page
     */
    @GetMapping("/payment/checkout")
    public String checkoutPage(
            @RequestParam(required = true) String paymentId,
            Model model) {
        
        log.info("Loading checkout page for payment: {}", paymentId);
        
        
        if (paymentId != null) {
            try {
                UUID paymentUuid = UUID.fromString(paymentId);
                System.out.println("Payment UUID: " + paymentUuid);
                Payment payment = paymentService.getPaymentById(paymentUuid, "123");
                System.out.println("Payment: " + payment);
                // Check if payment is in correct status
                if (!"INITIATED".equals(payment.getStatus())) {
                    log.warn("Payment {} is not in INITIATED status: {}", paymentId, payment.getStatus());
                    model.addAttribute("error", "Payment is not available for checkout");
                    return "error";
                }
                
                // Extract payment data from metadata (from decrypted signature)
                Map<String, Object> metadata = payment.getMetadata();
                
                // Set payment information for the checkout form
                model.addAttribute("paymentId", payment.getId());
                model.addAttribute("orderId", payment.getOrderId());
                model.addAttribute("amount", String.format("%.2f", payment.getAmountMinor() / 100.0));
                model.addAttribute("currency", payment.getCurrency());
                
                // Extract dynamic data from metadata (originally from JSON payload)
                model.addAttribute("productName", extractStringValue(metadata, "productName", "Premium Software License"));
                model.addAttribute("serviceName", extractStringValue(metadata, "serviceName", "Enterprise Support Package"));
                model.addAttribute("companyName", extractStringValue(metadata, "companyName", "AvinerTech Solutions"));
                model.addAttribute("supportName", "Customer Success Team");
                
                log.info("Successfully loaded payment data for checkout - Order: {}, Product: {}", 
                    payment.getOrderId(), payment.getProductName());
                
            } catch (Exception e) {
                log.error("Failed to load payment: {}", paymentId, e);
                model.addAttribute("error", "Payment not found or invalid");
                return "error";
            }
        } else {
            // Fallback to default values if no payment ID provided
            log.warn("No payment ID provided, using default values");
            model.addAttribute("productName", "Premium Software License");
            model.addAttribute("serviceName", "Enterprise Support Package");
            model.addAttribute("companyName", "AvinerTech Solutions");
            model.addAttribute("supportName", "Customer Success Team");
            model.addAttribute("amount", "299.99");
            model.addAttribute("currency", "USD");
        }
        
        return "checkout";
    }

    /**
     * Processes the checkout form submission and completes the payment.
     * 
     * @param formData The form data submitted by the user
     * @param request The HTTP request
     * @return Redirect to thank you page or error page
     */
    @PostMapping(value = "/payment/checkout", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String processCheckout(
            @RequestParam Map<String, String> formData,
            HttpServletRequest request) {
        
        log.info("Processing checkout with data: {}", formData);
        
        try {
            String paymentIdStr = formData.get("paymentId");
            if (paymentIdStr == null || paymentIdStr.trim().isEmpty()) {
                log.error("No payment ID in form data");
                return "redirect:/forms/payment/error?message=No payment ID provided";
            }
            
            UUID paymentId = UUID.fromString(paymentIdStr);
            String tenantId = (String) request.getAttribute("tenantId");
            
            // Complete the payment with form data
            Payment completedPayment = paymentService.completePayment(paymentId, tenantId, Map.copyOf(formData));
            
            log.info("Successfully completed payment: {} for order: {}", 
                completedPayment.getId(), 
                completedPayment.getOrderId());
            
            // Redirect to thank you page with payment ID
            return "redirect:/forms/payment/thankyou?paymentId=" + completedPayment.getId();
            
        } catch (Exception e) {
            log.error("Failed to complete payment: {}", formData.get("paymentId"), e);
            return "redirect:/forms/payment/error?message=Payment processing failed: " + e.getMessage();
        }
    }

    /**
     * Displays the thank you page after successful payment completion.
     * 
     * @param paymentId The payment UUID
     * @param model The Spring MVC model
     * @param request The HTTP request
     * @return The thank you page template or error page
     */
    @GetMapping("/payment/thankyou")
    public String thankYouPage(
            @RequestParam(required = false) String paymentId,
            Model model,
            HttpServletRequest request) {
        
        log.info("Loading thank you page for payment: {}", paymentId);
        
        if (paymentId != null) {
            try {
                String tenantId = (String) request.getAttribute("tenantId");
                UUID paymentUuid = UUID.fromString(paymentId);
                Payment payment = paymentService.getPaymentById(paymentUuid, tenantId);
                
                // Extract payment information for display
                Map<String, Object> metadata = payment.getMetadata();
                
                model.addAttribute("paymentId", payment.getId());
                model.addAttribute("amount", String.format("%.2f", payment.getAmountMinor() / 100.0));
                model.addAttribute("currency", payment.getCurrency());
                model.addAttribute("productName", extractStringValue(metadata, "productName", "Premium Software License"));
                model.addAttribute("serviceName", extractStringValue(metadata, "serviceName", "Enterprise Support Package"));
                model.addAttribute("companyName", extractStringValue(metadata, "companyName", "AvinerTech Solutions"));
                
                // Extract card information if available
                Map<String, Object> paymentData = (Map<String, Object>) metadata.get("paymentData");
                if (paymentData != null) {
                    String cardNumber = (String) paymentData.get("cardNumber");
                    if (cardNumber != null && cardNumber.length() >= 4) {
                        model.addAttribute("lastFourDigits", cardNumber.substring(cardNumber.length() - 4));
                    }
                }
                
                log.info("Successfully loaded thank you page data for payment: {}", paymentId);
                
            } catch (Exception e) {
                log.error("Failed to load payment for thank you page: {}", paymentId, e);
                model.addAttribute("error", "Payment information not available");
                return "error";
            }
        } else {
            log.warn("No payment ID provided for thank you page");
            model.addAttribute("error", "No payment information available");
            return "error";
        }
        
        return "thankyou";
    }

    /**
     * Displays error page with custom error message.
     * 
     * @param message The error message to display
     * @param model The Spring MVC model
     * @return The error page template
     */
    @GetMapping("/payment/error")
    public String errorPage(@RequestParam(required = false) String message, Model model) {
        log.warn("Displaying error page with message: {}", message);
        model.addAttribute("error", message != null ? message : "An error occurred");
        return "error";
    }

    /**
     * Extracts a string value from metadata with a default fallback.
     * 
     * @param metadata The metadata map
     * @param key The key to extract
     * @param defaultValue The default value if key not found
     * @return The string value or default value
     */
    private String extractStringValue(Map<String, Object> metadata, String key, String defaultValue) {
        Object value = metadata.get(key);
        return value != null ? value.toString() : defaultValue;
    }
}