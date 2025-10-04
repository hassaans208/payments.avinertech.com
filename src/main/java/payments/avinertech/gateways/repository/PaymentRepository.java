package payments.avinertech.gateways.repository;

import payments.avinertech.gateways.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    
    Optional<Payment> findByTenantIdAndProviderCodeAndProviderPaymentId(
            String tenantId, String providerCode, String providerPaymentId);
    
    Optional<Payment> findByTenantIdAndOrderIdAndClientReferenceId(
            String tenantId, String orderId, String clientReferenceId);
    
    Page<Payment> findByTenantIdAndStatus(String tenantId, String status, Pageable pageable);
    
    List<Payment> findByTenantIdAndStatus(String tenantId, String status);
    
    @Query("SELECT p FROM Payment p WHERE p.tenantId = :tenantId AND p.status IN :statuses")
    List<Payment> findByTenantIdAndStatusIn(@Param("tenantId") String tenantId, 
                                           @Param("statuses") List<String> statuses);
}
