package payments.avinertech.gateways.repository;

import payments.avinertech.gateways.entity.Payout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PayoutRepository extends JpaRepository<Payout, UUID> {
    
    Optional<Payout> findByTenantIdAndProviderCodeAndProviderPayoutId(
            UUID tenantId, String providerCode, String providerPayoutId);
    
    List<Payout> findByTenantIdAndStatus(UUID tenantId, String status);
    
    List<Payout> findByTenantIdAndBatchId(UUID tenantId, String batchId);
}
