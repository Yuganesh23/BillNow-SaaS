package com.example.Billing.subscription;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionWebhookEventRepository extends JpaRepository<SubscriptionWebhookEvent, Long> {
    boolean existsByEventId(String eventId);
}
