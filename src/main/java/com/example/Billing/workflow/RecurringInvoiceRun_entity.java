package com.example.Billing.workflow;
import com.example.Billing.invoice.Invoice_entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;

@Entity @Table(name = "recurring_invoice_runs", uniqueConstraints = @UniqueConstraint(name = "uk_recurring_run", columnNames = {"recurring_invoice_id", "scheduled_date"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RecurringInvoiceRun_entity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "recurring_invoice_id") private RecurringInvoice_entity recurringInvoice;
    @Column(name = "scheduled_date", nullable = false) private LocalDate scheduledDate;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "invoice_id") private Invoice_entity invoice;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @PrePersist void created() { if (createdAt == null) createdAt = LocalDateTime.now(); }
}
