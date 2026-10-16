package com.example.Billing.invoice;

import com.example.Billing.shop.Shop_entity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "document_sequences", uniqueConstraints =
        @UniqueConstraint(name = "uk_document_sequence", columnNames = {"shop_id", "financial_year", "document_type"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentSequence_entity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop_entity shop;

    @Column(name = "financial_year", nullable = false, length = 7)
    private String financialYear;

    @Column(name = "document_type", nullable = false, length = 20)
    private String documentType;

    @Column(name = "last_number", nullable = false)
    @Builder.Default
    private Integer lastNumber = 0;

    @Version
    private Long version;
}
