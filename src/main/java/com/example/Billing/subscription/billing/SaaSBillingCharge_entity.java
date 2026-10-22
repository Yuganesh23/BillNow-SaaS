package com.example.Billing.subscription.billing;

import com.example.Billing.shop.Shop_entity;
import com.example.Billing.subscription.SaaSPlan;
import com.example.Billing.subscription.SaaSSubscription_entity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One successful Razorpay subscription payment and the GST tax invoice that
 * BillNow issues to the shop for it. Supplier and recipient details are
 * snapshotted when the invoice is issued so later edits do not change it.
 */
@Entity
@Table(name = "saas_billing_charges", uniqueConstraints = {
        @UniqueConstraint(name = "uk_saas_charge_payment", columnNames = "razorpay_payment_id"),
        @UniqueConstraint(name = "uk_saas_charge_invoice_number", columnNames = "invoice_number")
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SaaSBillingCharge_entity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "shop_id", nullable = false)
    private Shop_entity shop;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "subscription_id", nullable = false)
    private SaaSSubscription_entity subscription;

    @Column(name = "razorpay_payment_id", nullable = false, length = 64) private String razorpayPaymentId;
    @Column(name = "razorpay_invoice_id", length = 64) private String razorpayInvoiceId;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private SaaSPlan plan;

    @Column(name = "paid_at", nullable = false) private LocalDateTime paidAt;
    @Column(name = "period_start") private LocalDateTime periodStart;
    @Column(name = "period_end") private LocalDateTime periodEnd;

    @Column(nullable = false, length = 3) private String currency;
    /** Amount actually charged by Razorpay, GST included. */
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2) private BigDecimal totalAmount;

    // Tax invoice, filled when the invoice is issued
    @Column(name = "invoice_number", length = 16) private String invoiceNumber;
    @Column(name = "financial_year", length = 7) private String financialYear;
    @Column(name = "invoice_issued_at") private LocalDateTime invoiceIssuedAt;
    @Column(name = "gst_rate", precision = 5, scale = 2) private BigDecimal gstRate;
    @Column(name = "taxable_amount", precision = 12, scale = 2) private BigDecimal taxableAmount;
    @Column(name = "cgst_amount", precision = 12, scale = 2) private BigDecimal cgstAmount;
    @Column(name = "sgst_amount", precision = 12, scale = 2) private BigDecimal sgstAmount;
    @Column(name = "igst_amount", precision = 12, scale = 2) private BigDecimal igstAmount;
    @Column(name = "sac_code", length = 8) private String sacCode;
    @Column(name = "supplier_legal_name", length = 150) private String supplierLegalName;
    @Column(name = "supplier_gstin", length = 15) private String supplierGstin;
    @Column(name = "supplier_address", length = 500) private String supplierAddress;
    @Column(name = "supplier_state_code", length = 2) private String supplierStateCode;
    @Column(name = "recipient_name", length = 150) private String recipientName;
    @Column(name = "recipient_gstin", length = 15) private String recipientGstin;
    @Column(name = "recipient_address", length = 500) private String recipientAddress;
    @Column(name = "place_of_supply_code", length = 2) private String placeOfSupplyCode;

    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;

    @PrePersist void beforeInsert() { if (createdAt == null) createdAt = LocalDateTime.now(); }
}
