package payments.avinertech.gateways.repository;

import payments.avinertech.gateways.entity.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, UUID> {
    
    List<JournalEntry> findByTenantIdAndRefTypeAndRefId(UUID tenantId, String refType, UUID refId);
    
    @Query("SELECT j FROM JournalEntry j WHERE j.tenantId = :tenantId ORDER BY j.createdAt DESC")
    List<JournalEntry> findByTenantIdOrderByCreatedAtDesc(@Param("tenantId") UUID tenantId);
}
