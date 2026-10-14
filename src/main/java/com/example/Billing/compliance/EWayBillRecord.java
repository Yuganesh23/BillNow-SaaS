package com.example.Billing.compliance;

import com.example.Billing.invoice.Invoice_entity;
import com.example.Billing.shop.Shop_entity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "e_way_bill_records", uniqueConstraints = {
        @UniqueConstraint(name = "uk_e_way_bill_record_invoice", columnNames = "invoice_id"),
        @UniqueConstraint(name = "uk_e_way_bill_record_shop_number", columnNames = {"shop_id", "e_way_bill_number"})
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class EWayBillRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "shop_id", nullable = false) private Shop_entity shop;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "invoice_id", nullable = false) private Invoice_entity invoice;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private ComplianceStatus status;
    @Column(name = "e_way_bill_number", length = 30) private String eWayBillNumber;
    @Column(name = "valid_until") private LocalDateTime validUntil;
    @Column(name = "transporter_id", length = 20) private String transporterId;
    @Column(name = "transporter_name", length = 100) private String transporterName;
    @Column(name = "transport_document_number", length = 30) private String transportDocumentNumber;
    @Column(name = "transport_document_date") private LocalDate transportDocumentDate;
    @Column(name = "transport_mode", length = 20) private String transportMode;
    @Column(name = "vehicle_number", length = 20) private String vehicleNumber;
    @Column(name = "vehicle_type", length = 20) private String vehicleType;
    @Column(name = "distance_km") private Integer distanceKm;
    @Column(name = "request_payload", nullable = false, columnDefinition = "TEXT") private String requestPayload;
    @Column(name = "response_payload", columnDefinition = "TEXT") private String responsePayload;
    @Column(name = "last_error", length = 1000) private String lastError;
    @Builder.Default @Column(name = "attempt_count", nullable = false) private Integer attemptCount = 0;
    @Column(name = "generated_at") private LocalDateTime generatedAt;
    @Column(name = "cancelled_at") private LocalDateTime cancelledAt;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

    @PrePersist void create() { LocalDateTime now = LocalDateTime.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void update() { updatedAt = LocalDateTime.now(); }
}
