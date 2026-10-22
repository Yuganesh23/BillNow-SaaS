package com.example.Billing.subscription.billing;

import jakarta.persistence.*;
import lombok.*;

/** Platform-wide tax invoice counter for BillNow's own subscription invoices. */
@Entity
@Table(name = "saas_invoice_sequences", uniqueConstraints =
        @UniqueConstraint(name = "uk_saas_invoice_sequence_fy", columnNames = "financial_year"))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SaaSInvoiceSequence_entity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "financial_year", nullable = false, length = 7) private String financialYear;
    @Column(name = "last_number", nullable = false) @Builder.Default private Integer lastNumber = 0;
    @Version private Long version;
}
