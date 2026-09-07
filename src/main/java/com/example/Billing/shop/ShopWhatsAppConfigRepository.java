package com.example.Billing.shop;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShopWhatsAppConfigRepository
        extends JpaRepository<ShopWhatsAppConfig, Long> {

    Optional<ShopWhatsAppConfig> findByShopId(Long shopId);

    boolean existsByShopId(Long shopId);
}