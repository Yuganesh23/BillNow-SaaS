package com.example.Billing.workflow;
import com.example.Billing.product.Product_entity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity @Table(name = "recurring_invoice_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RecurringInvoiceItem_entity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "recurring_invoice_id") private RecurringInvoice_entity recurringInvoice;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "product_id") private Product_entity product;
    @Column(nullable = false, precision = 12, scale = 3) private BigDecimal quantity;
    @Column(name = "line_discount", nullable = false, precision = 12, scale = 2) @Builder.Default private BigDecimal lineDiscount = BigDecimal.ZERO;
}
