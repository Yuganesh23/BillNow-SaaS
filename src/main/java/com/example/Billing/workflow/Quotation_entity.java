package com.example.Billing.workflow;

import com.example.Billing.auth.User_entity;
import com.example.Billing.customer.Customer_entity;
import com.example.Billing.invoice.Invoice_entity;
import com.example.Billing.shop.Shop_entity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name = "quotations", uniqueConstraints = @UniqueConstraint(name = "uk_quotation_number", columnNames = {"shop_id", "quote_number"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Quotation_entity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "shop_id") private Shop_entity shop;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "customer_id") private Customer_entity customer;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "created_by") private User_entity createdBy;
    @Column(name = "quote_number", nullable = false, length = 30) private String quoteNumber;
    @Enumerated(EnumType.STRING) @Column(name = "document_kind", nullable = false, length = 25) private WorkflowDocumentKind documentKind;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private QuotationStatus status;
    @Column(name = "valid_until") private LocalDate validUntil;
    @Column(name = "taxable_amount", nullable = false, precision = 12, scale = 2) private BigDecimal taxableAmount;
    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2) private BigDecimal discountAmount;
    @Column(name = "cgst_total", nullable = false, precision = 12, scale = 2) private BigDecimal cgstTotal;
    @Column(name = "sgst_total", nullable = false, precision = 12, scale = 2) private BigDecimal sgstTotal;
    @Column(name = "igst_total", nullable = false, precision = 12, scale = 2) private BigDecimal igstTotal;
    @Column(name = "cess_total", nullable = false, precision = 12, scale = 2) private BigDecimal cessTotal;
    @Column(name = "round_off", nullable = false, precision = 12, scale = 2) private BigDecimal roundOff;
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2) private BigDecimal totalAmount;
    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "converted_invoice_id") private Invoice_entity convertedInvoice;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @OneToMany(mappedBy = "quotation", cascade = CascadeType.ALL, orphanRemoval = true) @Builder.Default
    private List<QuotationItem_entity> items = new ArrayList<>();
    @PrePersist void created() { if (createdAt == null) createdAt = LocalDateTime.now(); }
}
