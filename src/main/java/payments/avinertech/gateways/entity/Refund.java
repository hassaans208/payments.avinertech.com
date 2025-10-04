package payments.avinertech.gateways.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "refund")
public class Refund {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;
    
    @Column(name = "provider_code", nullable = false)
    private String providerCode;
    
    @Column(name = "provider_refund_id")
    private String providerRefundId;
    
    @Column(name = "status", nullable = false)
    private String status;
    
    @Column(name = "amount_minor", nullable = false)
    private Long amountMinor;
    
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;
    
    @Column(name = "reason")
    private String reason;
    
    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
