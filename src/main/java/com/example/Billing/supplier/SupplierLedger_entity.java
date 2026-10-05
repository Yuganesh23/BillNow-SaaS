package com.example.Billing.supplier;

import com.example.Billing.shop.Shop_entity;
import com.example.Billing.purchase.Purchase_entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "supplier_ledger")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierLedger_entity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop_entity shop;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier_entity supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_id")
    private Purchase_entity purchase;

    @Column(nullable = false)
    private LocalDateTime transactionDate;

    @Column(nullable = false, length = 20)
    private String type; // "PURCHASE" or "PAYMENT"

    private BigDecimal debitAmount;  // Purchases (increases what we owe)
    private BigDecimal creditAmount; // Payments (decreases what we owe)
    
    private BigDecimal runningBalance;
    private String notes;
}
