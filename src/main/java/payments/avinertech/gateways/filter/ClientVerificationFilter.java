package payments.avinertech.gateways.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import payments.avinertech.gateways.service.ClientVerificationService;

import java.io.IOException;

@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class ClientVerificationFilter extends OncePerRequestFilter {
    
    private final ClientVerificationService verifier;
    
    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        
        // Skip verification for health checks, actuator endpoints, payments, and forms (for development)
        String path = req.getRequestURI();
        if (path.startsWith("/v1/health") || path.startsWith("/actuator") || 
            path.startsWith("/swagger-ui") || path.startsWith("/api-docs") ||
            path.startsWith("/v1/payments") || path.startsWith("/forms")) {
            chain.doFilter(req, res);
            return;
        }
        
        String encSub = extractEncryptedSubdomain(req);
        String sig = req.getHeader("X-APP-SIGNATURE");
        
        if (encSub == null || sig == null) {
            log.warn("Missing encrypted subdomain or signature for path: {}", path);
            res.sendError(403);
            return;
        }
        
        try {
            // var result = verifier.verify(encSub, sig);
            // req.setAttribute("tenantId", result.getTenantId());
            // MDC.put("tenantId", String.valueOf(result.getTenantId()));
            
            // For MySQL, tenant isolation is handled at application level
            // MySQL doesn't support session variables like PostgreSQL RLS
            
            chain.doFilter(req, res);
        } catch (Exception e) {
            log.error("Client verification failed", e);
            res.sendError(403);
        } finally {
            cleanup();
        }
    }
    
    private void cleanup() {
        MDC.remove("tenantId");
        // For MySQL, tenant cleanup is handled automatically since isolation 
        // is enforced at application level, not database session level
    }
    
    private String extractEncryptedSubdomain(HttpServletRequest req) {
        // Extract from X-ENC-SUB header (can be modified for path/host parsing)
        return req.getHeader("X-ENC-SUB");
    }
    
    private String extractTenantIdFromRequest(HttpServletRequest req) {
        try {
            // Read the request body
            java.io.BufferedReader reader = req.getReader();
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            
            // Parse JSON to extract tenantId
            String jsonBody = sb.toString();
            if (jsonBody.contains("\"tenantId\"")) {
                // Simple JSON parsing - extract tenantId value
                int start = jsonBody.indexOf("\"tenantId\":\"") + 12;
                int end = jsonBody.indexOf("\"", start);
                if (start > 11 && end > start) {
                    return jsonBody.substring(start, end);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to read request body for tenantId extraction", e);
        }
        return null;
    }
}
