package com.example.Billing.compliance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EWayBillRecordRepository extends JpaRepository<EWayBillRecord, Long> {
    Optional<EWayBillRecord> findByInvoiceIdAndShopId(Long invoiceId, Long shopId);
    List<EWayBillRecord> findByShopIdOrderByUpdatedAtDesc(Long shopId);
    List<EWayBillRecord> findTop100ByShopIdOrderByUpdatedAtDesc(Long shopId);
}
