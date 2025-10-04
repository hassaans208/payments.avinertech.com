package payments.avinertech.gateways.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "payment")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;
    
    @Column(name = "provider_code", nullable = false)
    private String providerCode;
    
    @Column(name = "provider_account_id")
    private UUID providerAccountId;
    
    @Column(name = "provider_payment_id")
    private String providerPaymentId;
    
    @Column(name = "status", nullable = false)
    private String status;
    
    @Column(name = "amount_minor", nullable = false)
    private Long amountMinor;
    
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;
    
    @Column(name = "method")
    private String method;
    
    @Column(name = "order_id")
    private String orderId;
    
    @Column(name = "product_name")
    private String productName;
    
    @Column(name = "service_host")
    private String serviceHost;
    
    @Column(name = "client_domain")
    private String clientDomain;
    
    @Column(name = "client_id")
    private String clientId;
    
    @Column(name = "client_reference_id")
    private String clientReferenceId;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "JSON")
    private Map<String, Object> metadata;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
