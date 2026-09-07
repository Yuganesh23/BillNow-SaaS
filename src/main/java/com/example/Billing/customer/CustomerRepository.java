package com.example.Billing.customer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository
        extends JpaRepository<Customer_entity, Long> {

    Optional<Customer_entity> findByShopIdAndWhatsappNumber(
            Long shopId,
            String whatsappNumber
    );

    Optional<Customer_entity> findByIdAndShopId(
            Long customerId,
            Long shopId
    );

    List<Customer_entity> findByShopId(Long shopId);

    long countByShopId(Long shopId);
}