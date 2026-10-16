package com.example.Billing.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog_entity, Long> {
    List<AuditLog_entity> findTop200ByShopIdOrderByCreatedAtDesc(Long shopId);
}
