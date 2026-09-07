package com.example.Billing.subscription;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SaaSSubscriptionRepository
        extends JpaRepository<SaaSSubscription_entity, Long> {

    Optional<SaaSSubscription_entity> findByShopId(Long shopId);

    Optional<SaaSSubscription_entity>
    findByRazorpaySubscriptionId(String razorpaySubscriptionId);

    boolean existsByShopId(Long shopId);

    boolean existsByRazorpaySubscriptionId(
            String razorpaySubscriptionId
    );
}