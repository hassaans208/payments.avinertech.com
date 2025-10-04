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
@Table(name = "webhook_event")
public class WebhookEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    @Column(name = "provider_code", nullable = false)
    private String providerCode;
    
    @Column(name = "provider_event_id", nullable = false)
    private String providerEventId;
    
    @Column(name = "type", nullable = false)
    private String type;
    
    @Column(name = "signature")
    private String signature;
    
    @Column(name = "payload_json", nullable = false, columnDefinition = "JSON")
    private String payloadJson;
    
    @CreationTimestamp
    @Column(name = "received_at", nullable = false, updatable = false)
    private OffsetDateTime receivedAt;
    
    @Column(name = "processed_at")
    private OffsetDateTime processedAt;
    
    @Column(name = "status", nullable = false)
    private String status = "RECEIVED";
    
    @Column(name = "dedupe_key", nullable = false)
    private String dedupeKey;
}
