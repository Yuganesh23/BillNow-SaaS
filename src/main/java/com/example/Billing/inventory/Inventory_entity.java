package com.example.Billing.inventory;

import com.example.Billing.product.Product_entity;
import com.example.Billing.shop.Shop_entity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_movements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory_entity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    // =====================================================
    // SHOP
    // =====================================================

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop_entity shop;


    // =====================================================
    // PRODUCT
    // =====================================================

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product_entity product;


    // =====================================================
    // MOVEMENT TYPE
    // =====================================================

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StockMovementType movementType;


    // =====================================================
    // QUANTITY
    // =====================================================

    @Column(nullable = false)
    private Integer quantity;


    // =====================================================
    // STOCK BEFORE
    // =====================================================

    @Column(nullable = false)
    private Integer stockBefore;


    // =====================================================
    // STOCK AFTER
    // =====================================================

    @Column(nullable = false)
    private Integer stockAfter;


    // =====================================================
    // REASON
    // =====================================================

    @Column(length = 500)
    private String reason;


    // =====================================================
    // CREATED AT
    // =====================================================

    @Column(nullable = false)
    private LocalDateTime createdAt;


    @PrePersist
    protected void onCreate() {

        createdAt = LocalDateTime.now();
    }
}