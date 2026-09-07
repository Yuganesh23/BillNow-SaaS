package com.example.Billing.inventory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryRepository
        extends JpaRepository<Inventory_entity, Long> {


    List<Inventory_entity>
    findByShopIdOrderByCreatedAtDesc(
            Long shopId
    );


    List<Inventory_entity>
    findByShopIdAndProductIdOrderByCreatedAtDesc(
            Long shopId,
            Long productId
    );
}