package com.example.Billing.product;

import java.math.BigDecimal;

import com.example.Billing.shop.Shop_entity;
import com.example.Billing.supplier.Supplier_entity;

import jakarta.persistence.*;
import lombok.*;
import com.example.Billing.product.TaxType;



@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product_entity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop_entity shop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier_entity supplier;


    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 100)
    private String sku;

    @Column(length = 100)
    private String category;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal purchasePrice;

        @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal sellingPrice;

    @Column(length = 20)
    private String hsnCode;

    @Column(precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal gstRate = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TaxType taxType = TaxType.NONE;

    @Column(nullable = false)
    private Double stockQuantity;

    @Column(columnDefinition = "TEXT")
    private String attributes;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Builder.Default
    @Column(name = "low_stock_threshold", nullable = false)
    private Double lowStockThreshold = 10.0;
}
