package com.example.Billing.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository
        extends JpaRepository<Payment_entity, Long> {

    Optional<Payment_entity> findFirstByInvoiceIdOrderByPaidAtDesc(Long invoiceId);

    List<Payment_entity> findAllByInvoiceIdOrderByPaidAtAsc(Long invoiceId);

    boolean existsByInvoiceId(
            Long invoiceId
    );

    List<Payment_entity> findByInvoiceShopId(
            Long shopId
    );
    Optional<Payment_entity> findByIdAndInvoiceShopId(Long id, Long shopId);

    @org.springframework.data.jpa.repository.Query("select coalesce(sum(p.amount), 0) from Payment_entity p where p.invoice.id = :invoiceId and p.invoice.shop.id = :shopId")
    java.math.BigDecimal sumByInvoiceAndShop(@org.springframework.data.repository.query.Param("invoiceId") Long invoiceId,
                                             @org.springframework.data.repository.query.Param("shopId") Long shopId);
}
