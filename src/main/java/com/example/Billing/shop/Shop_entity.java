package com.example.Billing.shop;

import com.example.Billing.auth.User_entity;
import com.example.Billing.subscription.SaaSSubscription_entity;
import jakarta.persistence.*;
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


    @Column(nullable = false, unique = true, length = 255)
    private String email;


    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;


    // =====================================================
    // OWNER
    // =====================================================

    @OneToOne
    @JoinColumn(
            name = "owner_id",
            nullable = false,
            unique = true
    )
    private User_entity owner;


    // =====================================================
    // SHOP USERS
    // =====================================================

    @OneToMany(mappedBy = "shop")
    @Builder.Default
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
    private ShopWhatsAppConfig whatsappConfig;

    @OneToOne(
            mappedBy = "shop",
            fetch = FetchType.LAZY
    )
    private SaaSSubscription_entity subscription;
}