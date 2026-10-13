package com.example.Billing.invoice;

import com.example.Billing.shop.Shop_entity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "document_notes", uniqueConstraints = @UniqueConstraint(name = "uk_document_note_number", columnNames = {"shop_id", "note_number"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DocumentNote_entity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "shop_id") private Shop_entity shop;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "original_invoice_id") private Invoice_entity originalInvoice;
    @Column(name = "note_number", nullable = false, length = 16) private String noteNumber;
    @Column(name = "note_type", nullable = false, length = 20) private String noteType;
    @Column(name = "financial_year", nullable = false, length = 7) private String financialYear;
    @Column(name = "series_number", nullable = false) private Integer seriesNumber;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    @Column(nullable = false, length = 500) private String reason;
    @Column(nullable = false) private boolean restock;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @PrePersist void created() { if (createdAt == null) createdAt = LocalDateTime.now(); }
}
