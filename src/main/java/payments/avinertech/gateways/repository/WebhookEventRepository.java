package payments.avinertech.gateways.repository;

import payments.avinertech.gateways.entity.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, UUID> {
    
    Optional<WebhookEvent> findByTenantIdAndProviderCodeAndProviderEventId(
            UUID tenantId, String providerCode, String providerEventId);
    
    Optional<WebhookEvent> findByTenantIdAndDedupeKey(UUID tenantId, String dedupeKey);
    
    @Query("SELECT w FROM WebhookEvent w WHERE w.tenantId = :tenantId AND w.status = 'RECEIVED'")
    List<WebhookEvent> findPendingEvents(@Param("tenantId") UUID tenantId);
}
