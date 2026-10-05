package com.example.Billing.shop;

import com.example.Billing.auth.User_entity;
import com.example.Billing.subscription.SaaSSubscription_entity;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

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

    @Column(length = 500)
    private String address;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String logoBase64;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SubscriptionTier subscriptionTier = SubscriptionTier.TRIAL;

    private java.time.LocalDateTime trialEndsAt;
    
    private java.time.LocalDateTime subscriptionEndsAt;


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