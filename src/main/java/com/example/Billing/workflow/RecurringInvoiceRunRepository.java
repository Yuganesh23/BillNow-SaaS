package com.example.Billing.workflow;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.List;
public interface RecurringInvoiceRunRepository extends JpaRepository<RecurringInvoiceRun_entity, Long> {
    List<RecurringInvoiceRun_entity> findByRecurringInvoiceShopIdOrderByCreatedAtDesc(Long shopId);
    Optional<RecurringInvoiceRun_entity> findByRecurringInvoiceIdAndScheduledDate(Long recurringId, LocalDate date);
}
