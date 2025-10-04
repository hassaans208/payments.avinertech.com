package payments.avinertech.gateways.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * Service responsible for decrypting signatures using the external signal service.
 * This service communicates with signal.avinertech.com/api/decrypt to decrypt
 * payment signatures and returns a JSON-encoded payload that contains all payment data.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SignatureDecryptionService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private static final String DECRYPT_API_URL = "https://signal.avinertech.com/api/decrypt";

    /**
     * Decrypts a signature and returns the payment data as a Map.
     * The signature decryption service returns a JSON-encoded payload that contains:
     * - tenantId, providerCode, providerAccountId
     * - amountMinor, currency, method, orderId
     * - productName, serviceName, companyName
     * - serviceHost, clientDomain, clientId, clientReferenceId
     * - callbackUrl, metadata
     * 
     * @param signature The encrypted signature to decrypt
     * @return Map containing decrypted payment data
     * @throws RuntimeException if decryption or JSON parsing fails
     */
    public Map<String, Object> decryptSignature(String signature) {
        try {
            log.info("Decrypting signature: {}", signature.substring(0, Math.min(10, signature.length())) + "...");
            
            // Prepare request body
            Map<String, String> requestBody = Map.of("value", signature);
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Create HTTP entity
            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);
            
            // Make API call to decrypt signature
            ResponseEntity<String> response = restTemplate.postForEntity(
                DECRYPT_API_URL, 
                request, 
                String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // Parse the response body as JSON to extract the "decrypted" field
                Map<String, Object> responseBodyMap = objectMapper.readValue(response.getBody(), Map.class);
                Object decryptedObj = responseBodyMap.get("decrypted");
                if (decryptedObj == null) {
                    throw new RuntimeException("Response does not contain 'decrypted' field");
                }
                String jsonPayload = decryptedObj.toString();
                log.info("Successfully decrypted signature. Received JSON payload: {} characters", jsonPayload.length());

                // Parse the JSON payload into a Map
                Map<String, Object> decryptedData = objectMapper.readValue(jsonPayload, Map.class);
                log.info("Successfully parsed JSON payload. Extracted {} fields", decryptedData.size());

                return decryptedData;
            } else {
                throw new RuntimeException("Failed to decrypt signature. Response status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error decrypting signature", e);
            throw new RuntimeException("Failed to decrypt signature: " + e.getMessage(), e);
        }
    }
}
