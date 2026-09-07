package com.example.Billing.subscription;

import com.example.Billing.shop.Shop_entity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "saas_subscriptions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_saas_subscription_shop",
                        columnNames = "shop_id"
                ),
                @UniqueConstraint(
                        name = "uk_saas_subscription_razorpay_id",
                        columnNames = "razorpay_subscription_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaaSSubscription_entity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * One shop can have one SaaS subscription.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "shop_id",
            nullable = false,
            unique = true
    )
    private Shop_entity shop;

    /*
     * Our SaaS plan
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SaaSPlan plan;

    /*
     * Razorpay IDs
     */
    @Column(name = "razorpay_customer_id")
    private String razorpayCustomerId;

    @Column(
            name = "razorpay_subscription_id",
            nullable = false,
            unique = true
    )
    private String razorpaySubscriptionId;

    /*
     * Current subscription status
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SubscriptionStatus status;

    /*
     * Billing dates
     */
    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private LocalDateTime nextBillingDate;

    /*
     * Whether SaaS access is currently allowed
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean active = false;

    /*
     * Razorpay payment ID of authorization payment
     */
    private String razorpayPaymentId;

    /*
     * Last updated
     */
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}