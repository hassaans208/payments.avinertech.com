package payments.avinertech.gateways.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for payment creation requests.
 * This request now uses a signature-based approach where all payment data
 * is encrypted in the signature and will be decrypted by the SignatureDecryptionService.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCreateRequest {
    
    /**
     * The encrypted signature containing all payment data.
     * This signature will be decrypted to extract:
     * - packageId
     * - productName
     * - serviceId
     * - amount
     * - currency
     * - tenantId
     * - and other payment-related information
     */
    private String signature;
}