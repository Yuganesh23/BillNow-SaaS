package com.example.Billing.product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository
        extends JpaRepository<Product_entity, Long> {

    // Get all products of a shop
    List<Product_entity> findByShopId(Long shopId);


    // Get only active products of a shop
    List<Product_entity> findByShopIdAndActiveTrue(Long shopId);


    // Get a specific product belonging to a specific shop
    Optional<Product_entity> findByIdAndShopId(
            Long productId,
            Long shopId
    );


    // Check duplicate SKU inside a shop
    boolean existsByShopIdAndSku(
            Long shopId,
            String sku
    );


    // Dashboard - total products
    long countByShopIdAndActiveTrue(Long shopId);


    // Dashboard - products below/equal fixed threshold
    @Query("SELECT COUNT(p) FROM Product_entity p WHERE p.shop.id = :shopId AND p.active = true AND p.stockQuantity <= p.lowStockThreshold")
    long countLowStockProducts(@Param("shopId") Long shopId);
}