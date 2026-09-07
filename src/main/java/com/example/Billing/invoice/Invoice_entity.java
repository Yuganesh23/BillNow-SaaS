package com.example.Billing.invoice;

import com.example.Billing.auth.User_entity;
import com.example.Billing.customer.Customer_entity;
import com.example.Billing.invoice.whatsapp.WhatsAppStatus;
import com.example.Billing.shop.Shop_entity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "invoices",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_shop_invoice_number",
                        columnNames = {
                                "shop_id",
                                "invoiceNumber"
                        }
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice_entity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    // =====================================================
    // SHOP
    // =====================================================

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "shop_id",
            nullable = false
    )
    private Shop_entity shop;


    // =====================================================
    // CUSTOMER
    // =====================================================

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "customer_id",
            nullable = false
    )
    private Customer_entity customer;


    // =====================================================
    // BILLER
    // =====================================================

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "biller_id",
            nullable = false
    )
    private User_entity biller;


    // =====================================================
    // INVOICE NUMBER
    // =====================================================

    @Column(
            nullable = false,
            length = 50
    )
    private String invoiceNumber;


    // =====================================================
    // AMOUNTS
    // =====================================================

    @Column(
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal subtotal;


    @Column(
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal discountAmount;


    @Column(
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal totalAmount;


    // =====================================================
    // INVOICE STATUS
    // =====================================================

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 20
    )
    private InvoiceStatus status;


    // =====================================================
    // WHATSAPP STATUS
    // =====================================================

    @Enumerated(EnumType.STRING)
    @Column(
            name = "whatsapp_status",
            nullable = false,
            length = 20
    )
    @Builder.Default
    private WhatsAppStatus whatsappStatus =
            WhatsAppStatus.NOT_SENT;


    // =====================================================
    // WHATSAPP SENT TIME
    // =====================================================

    @Column(
            name = "whatsapp_sent_at"
    )
    private LocalDateTime whatsappSentAt;


    // =====================================================
    // WHATSAPP ERROR
    // =====================================================

    @Column(
            name = "whatsapp_error",
            columnDefinition = "TEXT"
    )
    private String whatsappError;


    // =====================================================
    // CREATED AT
    // =====================================================

    @Column(
            nullable = false
    )
    private LocalDateTime createdAt;


    // =====================================================
    // INVOICE ITEMS
    // =====================================================

    @OneToMany(
            mappedBy = "invoice",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<InvoiceItem> items =
            new ArrayList<>();


    // =====================================================
    // PRE PERSIST
    // =====================================================

    @PrePersist
    protected void onCreate() {

        createdAt =
                LocalDateTime.now();

        if (whatsappStatus == null) {

            whatsappStatus =
                    WhatsAppStatus.NOT_SENT;
        }
    }
}
