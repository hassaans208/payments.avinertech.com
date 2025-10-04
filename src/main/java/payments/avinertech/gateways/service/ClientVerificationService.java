package payments.avinertech.gateways.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClientVerificationService {
    
    private final WebClient clientVerifyWebClient;
    
    public VerificationResult verify(String encryptedSubdomain, String signature) {
        return clientVerifyWebClient.get()
                .uri(uri -> uri.path("/api/verify/{enc}/signature").build(encryptedSubdomain))
                .header("X-APP-SIGNATURE", signature)
                .retrieve()
                .onStatus(s -> !s.is2xxSuccessful(), r -> 
                    Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN)))
                .bodyToMono(VerificationResult.class)
                .block();
    }
    
    @Data
    public static class VerificationResult {
        private UUID tenantId;
        private String clientDomain;
        private String clientId;
    }
}
