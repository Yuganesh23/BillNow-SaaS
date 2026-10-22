package com.example.Billing.workflow;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.*;

public interface RecurringInvoiceRepository extends JpaRepository<RecurringInvoice_entity, Long> {
    List<RecurringInvoice_entity> findByShopIdOrderByIdDesc(Long shopId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RecurringInvoice_entity r where r.id=:id and r.shop.id=:shopId")
    Optional<RecurringInvoice_entity> findByIdAndShopIdForUpdate(@Param("id") Long id, @Param("shopId") Long shopId);
    @Query("select r.id from RecurringInvoice_entity r where r.active=true and r.nextRunDate<=:today")
    List<Long> findDueIds(@Param("today") LocalDate today);
}
