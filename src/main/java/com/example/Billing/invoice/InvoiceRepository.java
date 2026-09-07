package com.example.Billing.invoice;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository  extends JpaRepository<Invoice_entity, Long> {

    List<Invoice_entity> findByShopIdOrderByCreatedAtDesc(Long shopId);

    Optional<Invoice_entity> findByIdAndShopId(
            Long invoiceId,
            Long shopId
    );
}
