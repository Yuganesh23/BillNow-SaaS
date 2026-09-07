package com.example.Billing.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository
        extends JpaRepository<Payment_entity, Long> {

    Optional<Payment_entity> findByInvoiceId(
            Long invoiceId
    );

    boolean existsByInvoiceId(
            Long invoiceId
    );

    List<Payment_entity> findByInvoiceShopId(
            Long shopId
    );
}