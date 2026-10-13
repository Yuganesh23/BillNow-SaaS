package com.example.Billing.workflow;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;
public interface RecurringInvoiceRunRepository extends JpaRepository<RecurringInvoiceRun_entity, Long> {
    Optional<RecurringInvoiceRun_entity> findByRecurringInvoiceIdAndScheduledDate(Long recurringId, LocalDate date);
}
