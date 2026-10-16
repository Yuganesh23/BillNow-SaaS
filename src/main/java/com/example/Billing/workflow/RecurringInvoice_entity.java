package com.example.Billing.workflow;

import com.example.Billing.auth.User_entity;
import com.example.Billing.customer.Customer_entity;
import com.example.Billing.product.Product_entity;
import com.example.Billing.shop.Shop_entity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Entity @Table(name = "recurring_invoices")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RecurringInvoice_entity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "shop_id") private Shop_entity shop;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "customer_id") private Customer_entity customer;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "created_by") private User_entity createdBy;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private RecurringFrequency frequency;
    @Column(name = "next_run_date", nullable = false) private LocalDate nextRunDate;
    @Column(name = "issue_automatically", nullable = false) private boolean issueAutomatically;
    @Column(nullable = false) @Builder.Default private boolean active = true;
    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2) @Builder.Default private BigDecimal discountAmount = BigDecimal.ZERO;
    @OneToMany(mappedBy = "recurringInvoice", cascade = CascadeType.ALL, orphanRemoval = true) @Builder.Default
    private List<RecurringInvoiceItem_entity> items = new ArrayList<>();
}
