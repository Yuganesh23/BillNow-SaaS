package com.example.Billing.subscription;

import com.example.Billing.shop.Shop_entity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscription_webhook_events", uniqueConstraints =
        @UniqueConstraint(name = "uk_subscription_webhook_event_id", columnNames = "event_id"))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SubscriptionWebhookEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "shop_id", nullable = false) private Shop_entity shop;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "subscription_id", nullable = false) private SaaSSubscription_entity subscription;
    @Column(name = "event_id", nullable = false, length = 128) private String eventId;
    @Column(name = "event_type", nullable = false, length = 80) private String eventType;
    @Column(name = "payload_hash", nullable = false, length = 64) private String payloadHash;
    @Column(name = "processed_at", nullable = false) private LocalDateTime processedAt;

    @PrePersist void beforeInsert() { if (processedAt == null) processedAt = LocalDateTime.now(); }
}
