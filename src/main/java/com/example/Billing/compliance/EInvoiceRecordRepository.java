package com.example.Billing.compliance;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface EInvoiceRecordRepository extends JpaRepository<EInvoiceRecord, Long> {
    Optional<EInvoiceRecord> findByInvoiceIdAndShopId(Long invoiceId, Long shopId);
    List<EInvoiceRecord> findByShopIdOrderByUpdatedAtDesc(Long shopId);
    List<EInvoiceRecord> findTop100ByShopIdOrderByUpdatedAtDesc(Long shopId);
}
