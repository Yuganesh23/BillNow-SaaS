package com.example.Billing.shop;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShopRepository extends JpaRepository<Shop_entity, Long> {

    Optional<Shop_entity> findByEmail(String email);

    boolean existsByEmail(String email);

}