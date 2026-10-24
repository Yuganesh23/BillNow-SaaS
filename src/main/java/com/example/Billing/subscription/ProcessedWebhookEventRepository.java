package com.example.Billing.subscription;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProcessedWebhookEventRepository extends JpaRepository<ProcessedWebhookEvent_entity, Long> {
    boolean existsByEventId(String eventId);
    Optional<ProcessedWebhookEvent_entity> findByEventId(String eventId);
}
