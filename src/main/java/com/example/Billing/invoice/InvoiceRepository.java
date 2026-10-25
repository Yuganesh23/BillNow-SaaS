package com.example.Billing.invoice;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;

public interface InvoiceRepository  extends JpaRepository<Invoice_entity, Long> {

    List<Invoice_entity> findByShopIdOrderByCreatedAtDesc(Long shopId);

    Optional<Invoice_entity> findByIdAndShopId(
            Long invoiceId,
            Long shopId
    );

    @Query("SELECT COUNT(i) FROM Invoice_entity i WHERE i.biller.id = :billerId")
    long countByBillerId(@Param("billerId") Long billerId);

    @Query("SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice_entity i WHERE i.biller.id = :billerId")
    BigDecimal sumTotalAmountByBillerId(@Param("billerId") Long billerId);
    @Query("SELECT COUNT(i) FROM Invoice_entity i WHERE i.biller.id = :billerId AND i.shop.id = :shopId")
    long countByBillerIdAndShopId(@Param("billerId") Long billerId, @Param("shopId") Long shopId);

    @Query("SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice_entity i WHERE i.biller.id = :billerId AND i.shop.id = :shopId")
    BigDecimal sumTotalAmountByBillerIdAndShopId(@Param("billerId") Long billerId, @Param("shopId") Long shopId);

    @Query("SELECT i FROM Invoice_entity i LEFT JOIN FETCH i.customer c LEFT JOIN FETCH i.biller b WHERE i.shop.id = :shopId AND i.status <> com.example.Billing.invoice.InvoiceStatus.CANCELLED AND i.createdAt >= :from AND i.createdAt < :to ORDER BY i.createdAt DESC")
    List<Invoice_entity> findGstInvoicesForExport(@Param("shopId") Long shopId, @Param("from") java.time.LocalDateTime from, @Param("to") java.time.LocalDateTime to);
}
