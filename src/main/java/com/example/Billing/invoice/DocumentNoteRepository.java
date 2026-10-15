package com.example.Billing.invoice;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DocumentNoteRepository extends JpaRepository<DocumentNote_entity, Long> {
    List<DocumentNote_entity> findByShopIdOrderByCreatedAtDesc(Long shopId);
    List<DocumentNote_entity> findByShopIdAndOriginalInvoiceIdOrderByCreatedAtDesc(Long shopId, Long invoiceId);
}
