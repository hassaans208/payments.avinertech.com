package payments.avinertech.gateways.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "idempotency_key")
public class IdempotencyKey {
    
    @Id
    @Column(name = "`key`")
    private String key;
    
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    @Column(name = "scope", nullable = false)
    private String scope;
    
    @Column(name = "request_hash", nullable = false)
    private String requestHash;
    
    @Column(name = "response_json", columnDefinition = "JSON")
    private String responseJson;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    
    @Column(name = "ttl_seconds", nullable = false)
    private Integer ttlSeconds = 86400;
}
