package com.example.Billing.supplier;

import com.example.Billing.shop.Shop_entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "suppliers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Supplier_entity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop_entity shop;

    @Column(nullable = false, length = 150)
    private String businessName;

    @Column(length = 150)
    private String contactPerson;

    @Column(length = 20)
    private String phone;

    @Column(length = 20)
    private String whatsapp;

    @Column(length = 255)
    private String email;

    @Column(length = 50)
    private String gstin;

    @Column(length = 100)
    private String state;

    @Column(length = 500)
    private String address;

    @Column(length = 1000)
    private String notes;

    private BigDecimal totalPurchases;
    private BigDecimal amountPaid;
    private BigDecimal outstandingBalance;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (totalPurchases == null) totalPurchases = BigDecimal.ZERO;
        if (amountPaid == null) amountPaid = BigDecimal.ZERO;
        if (outstandingBalance == null) outstandingBalance = BigDecimal.ZERO;
    }
}
