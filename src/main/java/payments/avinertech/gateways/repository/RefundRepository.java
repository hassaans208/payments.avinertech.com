package payments.avinertech.gateways.repository;

import payments.avinertech.gateways.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefundRepository extends JpaRepository<Refund, UUID> {
    
    Optional<Refund> findByTenantIdAndProviderCodeAndProviderRefundId(
            UUID tenantId, String providerCode, String providerRefundId);
    
    List<Refund> findByTenantIdAndPaymentId(UUID tenantId, UUID paymentId);
    
    List<Refund> findByTenantIdAndStatus(UUID tenantId, String status);
}
