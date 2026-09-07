package com.example.Billing.shop;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "shop_whatsapp_configs",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_whatsapp_shop",
                        columnNames = "shop_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShopWhatsAppConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    // =====================================================
    // SHOP
    // =====================================================

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "shop_id",
            nullable = false,
            unique = true
    )
    private Shop_entity shop;


    // =====================================================
    // META WHATSAPP CONFIG
    // =====================================================

    @Column(
            name = "phone_number_id",
            nullable = false,
            length = 100
    )
    private String phoneNumberId;


    @Column(
            name = "business_account_id",
            length = 100
    )
    private String businessAccountId;


    @Column(
            name = "access_token",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String accessToken;


    // =====================================================
    // STATUS
    // =====================================================

    @Builder.Default
    @Column(nullable = false)
    private boolean enabled = true;


    // =====================================================
    // AUDIT
    // =====================================================

    @Column(nullable = false)
    private LocalDateTime createdAt;


    @Column(nullable = false)
    private LocalDateTime updatedAt;


    // =====================================================
    // CREATE
    // =====================================================

    @PrePersist
    protected void onCreate() {

        LocalDateTime now =
                LocalDateTime.now();

        createdAt = now;
        updatedAt = now;
    }


    // =====================================================
    // UPDATE
    // =====================================================

    @PreUpdate
    protected void onUpdate() {

        updatedAt =
                LocalDateTime.now();
    }
}
