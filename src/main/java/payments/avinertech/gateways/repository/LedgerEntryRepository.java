package payments.avinertech.gateways.repository;

import payments.avinertech.gateways.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {
    
    List<LedgerEntry> findByTenantIdAndAccountId(UUID tenantId, UUID accountId);
    
    @Query("SELECT l FROM LedgerEntry l WHERE l.tenantId = :tenantId ORDER BY l.createdAt DESC")
    List<LedgerEntry> findByTenantIdOrderByCreatedAtDesc(@Param("tenantId") UUID tenantId);
}
