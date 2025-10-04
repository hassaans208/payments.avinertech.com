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
@Table(name = "payout")
public class Payout {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    
    @Column(name = "payee_label")
    private String payeeLabel;
    
    @Column(name = "provider_code", nullable = false)
    private String providerCode;
    
    @Column(name = "provider_account_id")
    private UUID providerAccountId;
    
    @Column(name = "provider_payout_id")
    private String providerPayoutId;
    
    @Column(name = "status", nullable = false)
    private String status;
    
    @Column(name = "batch_id")
    private String batchId;
    
    @Column(name = "amount_minor", nullable = false)
    private Long amountMinor;
    
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
