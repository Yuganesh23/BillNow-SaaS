package com.example.Billing.shop;

import com.example.Billing.auth.User_entity;
import com.example.Billing.subscription.SaaSSubscription_entity;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import com.example.Billing.gst.RegistrationType;

@Entity
@Table(name = "shops")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shop_entity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    // =====================================================
    // SHOP DETAILS
    // =====================================================

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "invoice_name", length = 150)
    private String invoiceName;


    @Column(nullable = false, unique = true, length = 255)
    private String email;

        @Column(length = 20)
    private String mobileNumber;

    @Column(length = 50)
    private String gstin;

    @Column(length = 150)
    private String legalName;

    @Column(length = 100)
    private String state;

    @Column(name = "state_code", nullable = false, length = 2)
    @Builder.Default
    private String stateCode = "97";

    @Enumerated(EnumType.STRING)
    @Column(name = "registration_type", nullable = false, length = 20)
    @Builder.Default
    private RegistrationType registrationType = RegistrationType.UNREGISTERED;

    @Column(name = "hsn_code_length", nullable = false)
    @Builder.Default
    private Integer hsnCodeLength = 4;

    @Column(name = "invoice_prefix", nullable = false, length = 8)
    @Builder.Default
    private String invoicePrefix = "INV";

    @Column(name = "credit_note_prefix", nullable = false, length = 8)
    @Builder.Default
    private String creditNotePrefix = "CN";

    @Column(name = "debit_note_prefix", nullable = false, length = 8)
    @Builder.Default
    private String debitNotePrefix = "DN";

    @Column(name = "bank_details", columnDefinition = "TEXT")
    private String bankDetails;

    @Column(name = "invoice_terms", columnDefinition = "TEXT")
    private String invoiceTerms;

    @Column(name = "upi_id", length = 100)
    private String upiId;

    @Column(length = 500)
    private String address;

    @Column(length = 100)
    private String locality;

    @Column(length = 6)
    private String pincode;

    @Builder.Default
    @Column(name = "e_invoice_enabled", nullable = false)
    private boolean eInvoiceEnabled = false;

    @Builder.Default
    @Column(name = "e_way_bill_enabled", nullable = false)
    private boolean eWayBillEnabled = false;

    @Builder.Default
    @Column(name = "e_invoice_cancellation_window_hours", nullable = false)
    private Integer eInvoiceCancellationWindowHours = 24;

    @Column(columnDefinition = "TEXT")
    private String logoBase64;

    @Column(name = "logo_object_key", length = 500)
    private String logoObjectKey;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SubscriptionTier subscriptionTier = SubscriptionTier.TRIAL;

    private java.time.LocalDateTime trialEndsAt;
    
    private java.time.LocalDateTime subscriptionEndsAt;

    @Builder.Default
    @Column(nullable = false)
    private Integer lastInvoiceNumber = 0;

    @Version
    private Long version;


    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;


    // =====================================================
    // OWNER
    // =====================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "owner_id",
            nullable = false
    )
    @JsonIgnore
    private User_entity owner;


    // =====================================================
    // SHOP USERS
    // =====================================================

    @OneToMany(mappedBy = "shop")
    @Builder.Default
    @JsonIgnore
    private List<User_entity> users =
            new ArrayList<>();


    // =====================================================
    // WHATSAPP CONFIG
    // =====================================================

    @OneToOne(
            mappedBy = "shop",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @JsonIgnore
    private ShopWhatsAppConfig whatsappConfig;

    @OneToOne(
            mappedBy = "shop",
            fetch = FetchType.LAZY
    )
    @JsonIgnore
    private SaaSSubscription_entity subscription;
}
