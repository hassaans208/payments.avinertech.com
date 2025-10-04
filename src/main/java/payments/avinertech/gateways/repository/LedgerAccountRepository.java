package payments.avinertech.gateways.repository;

import payments.avinertech.gateways.entity.LedgerAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LedgerAccountRepository extends JpaRepository<LedgerAccount, UUID> {
    
    Optional<LedgerAccount> findByTenantIdAndCodeAndCurrency(UUID tenantId, String code, String currency);
}
