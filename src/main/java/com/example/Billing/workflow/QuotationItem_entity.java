package com.example.Billing.workflow;

import com.example.Billing.product.Product_entity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity @Table(name = "quotation_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class QuotationItem_entity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "quotation_id") private Quotation_entity quotation;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "product_id") private Product_entity product;
    @Column(nullable = false, precision = 12, scale = 3) private BigDecimal quantity;
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2) private BigDecimal unitPrice;
    @Column(name = "line_discount", nullable = false, precision = 12, scale = 2) private BigDecimal lineDiscount;
    @Column(name = "taxable_amount", nullable = false, precision = 12, scale = 2) private BigDecimal taxableAmount;
    @Column(name = "gst_rate", nullable = false, precision = 5, scale = 2) private BigDecimal gstRate;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal cgst;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal sgst;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal igst;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal cess;
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2) private BigDecimal totalAmount;
}
