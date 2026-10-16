package com.example.Billing.compliance;

import com.example.Billing.invoice.Invoice_entity;
import com.example.Billing.shop.Shop_entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "e_invoice_records", uniqueConstraints = {
        @UniqueConstraint(name = "uk_e_invoice_record_invoice", columnNames = "invoice_id"),
        @UniqueConstraint(name = "uk_e_invoice_record_shop_irn", columnNames = {"shop_id", "irn"})
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class EInvoiceRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "shop_id", nullable = false) private Shop_entity shop;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "invoice_id", nullable = false) private Invoice_entity invoice;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private ComplianceStatus status;
    @Column(name = "provider_reference", length = 100) private String providerReference;
    @Column(length = 100) private String irn;
    @Column(name = "acknowledgement_number", length = 100) private String acknowledgementNumber;
    @Column(name = "acknowledgement_date") private LocalDateTime acknowledgementDate;
    @Column(name = "signed_qr_data", columnDefinition = "TEXT") private String signedQrData;
    @Column(name = "request_payload", nullable = false, columnDefinition = "TEXT") private String requestPayload;
    @Column(name = "response_payload", columnDefinition = "TEXT") private String responsePayload;
    @Column(name = "last_error", length = 1000) private String lastError;
    @Builder.Default @Column(name = "attempt_count", nullable = false) private Integer attemptCount = 0;
    @Column(name = "generated_at") private LocalDateTime generatedAt;
    @Column(name = "cancelled_at") private LocalDateTime cancelledAt;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

    @PrePersist void create() { LocalDateTime now=LocalDateTime.now(); createdAt=now; updatedAt=now; }
    @PreUpdate void update() { updatedAt=LocalDateTime.now(); }
}
