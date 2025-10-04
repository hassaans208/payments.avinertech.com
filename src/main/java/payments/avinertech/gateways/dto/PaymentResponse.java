package payments.avinertech.gateways.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import payments.avinertech.gateways.entity.Payment;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    
    private UUID id;
    private String tenantId;
    private String providerCode;
    private UUID providerAccountId;
    private String providerPaymentId;
    private String status;
    private Long amountMinor;
    private String currency;
    private String method;
    private String orderId;
    private String productName;
    private String serviceName;
    private String companyName;
    private String serviceHost;
    private String clientDomain;
    private String clientId;
    private String clientReferenceId;
    private String callbackUrl;
    private String checkoutUrl;
    private Map<String, Object> metadata;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private String error;

    public static PaymentResponse fromPayment(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .tenantId(payment.getTenantId())
                .providerCode(payment.getProviderCode())
                .providerAccountId(payment.getProviderAccountId())
                .providerPaymentId(payment.getProviderPaymentId())
                .status(payment.getStatus())
                .amountMinor(payment.getAmountMinor())
                .currency(payment.getCurrency())
                .method(payment.getMethod())
                .orderId(payment.getOrderId())
                .productName(payment.getProductName())
                .serviceName(extractServiceName(payment.getMetadata()))
                .companyName(extractCompanyName(payment.getMetadata()))
                .serviceHost(payment.getServiceHost())
                .clientDomain(payment.getClientDomain())
                .clientId(payment.getClientId())
                .clientReferenceId(payment.getClientReferenceId())
                .callbackUrl(extractCallbackUrl(payment.getMetadata()))
                .checkoutUrl(buildCheckoutUrl(payment.getId()))
                .metadata(parseMetadata(payment.getMetadata()))
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }

    public static PaymentResponse error(String errorMessage) {
        return PaymentResponse.builder()
                .error(errorMessage)
                .build();
    }

    private static String extractCallbackUrl(Map<String, Object> metadata) {
        // Extract callback URL from metadata Map
        if (metadata != null) {
            Object callbackUrl = metadata.get("callbackUrl");
            return callbackUrl != null ? callbackUrl.toString() : null;
        }
        return null;
    }

    private static String extractServiceName(Map<String, Object> metadata) {
        // Extract service name from metadata Map
        if (metadata != null) {
            Object serviceName = metadata.get("serviceName");
            return serviceName != null ? serviceName.toString() : null;
        }
        return null;
    }

    private static String extractCompanyName(Map<String, Object> metadata) {
        // Extract company name from metadata Map
        if (metadata != null) {
            Object companyName = metadata.get("companyName");
            return companyName != null ? companyName.toString() : null;
        }
        return null;
    }

    private static String buildCheckoutUrl(UUID paymentId) {
        return "https://payments.avinertech.com/forms/payment/checkout?paymentId=" + paymentId.toString();
    }

    private static Map<String, Object> parseMetadata(Map<String, Object> metadata) {
        // Return the metadata Map as-is since it's already parsed
        return metadata != null ? metadata : Map.of();
    }
}
