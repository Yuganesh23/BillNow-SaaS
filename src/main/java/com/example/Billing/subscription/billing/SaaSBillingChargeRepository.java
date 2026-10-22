package com.example.Billing.subscription.billing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SaaSBillingChargeRepository extends JpaRepository<SaaSBillingCharge_entity, Long> {
    boolean existsByRazorpayPaymentId(String razorpayPaymentId);
    List<SaaSBillingCharge_entity> findByShopIdOrderByPaidAtDescIdDesc(Long shopId);
    Optional<SaaSBillingCharge_entity> findByIdAndShopId(Long id, Long shopId);
}
