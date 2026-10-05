package com.example.Billing.inventory;

import com.example.Billing.auth.User_entity;
import com.example.Billing.product.ProductRepository;
import com.example.Billing.product.Product_entity;
import com.example.Billing.shop.Shop_entity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.example.Billing.config.ShopContextResolver;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ShopContextResolver shopContextResolver;
    private final ProductRepository productRepository;


    // =====================================================
    // ADJUST STOCK
    // =====================================================

    public InventoryResponse_Dto adjustStock(
            User_entity user,
            StockAdjustmentRequest_Dto request
    ) {

        Shop_entity shop =
                getUserShop(user);

        Long shopId =
                shop.getId();


        // =================================================
        // GET PRODUCT
        // =================================================

        Product_entity product =
                productRepository
                        .findByIdAndShopId(
                                request.getProductId(),
                                shopId
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Product not found in this shop"
                                )
                        );


        // =================================================
        // VALIDATE QUANTITY
        // =================================================

        Double quantity =
                request.getQuantity();

        if (quantity == null ||
                quantity == 0) {

            throw new RuntimeException(
                    "Quantity cannot be zero"
            );
        }


        // =================================================
        // CURRENT STOCK
        // =================================================

        Double currentStock =
                product.getStockQuantity();


        if (currentStock == null) {

            currentStock = 0.0;
        }


        // =================================================
        // CALCULATE NEW STOCK
        // =================================================

        Double newStock =
                currentStock + quantity;


        if (newStock < 0) {

            throw new RuntimeException(
                    "Stock cannot be negative. "
                            + "Available stock: "
                            + currentStock
            );
        }


        // =================================================
        // UPDATE PRODUCT
        // =================================================

        product.setStockQuantity(
                newStock
        );

        productRepository.save(product);


        // =================================================
        // MOVEMENT TYPE
        // =================================================

        StockMovementType movementType;


        if (quantity > 0) {

            movementType =
                    StockMovementType.STOCK_IN;

        } else {

            movementType =
                    StockMovementType.STOCK_OUT;
        }


        // =================================================
        // CREATE MOVEMENT
        // =================================================

        Inventory_entity movement =
                Inventory_entity.builder()
                        .shop(shop)
                        .product(product)
                        .movementType(movementType)
                        .quantity(Math.abs(quantity))
                        .stockBefore(currentStock)
                        .stockAfter(newStock)
                        .reason(request.getReason())
                        .build();


        inventoryRepository.save(
                movement
        );


        return mapInventoryResponse(
                product
        );
    }


    // =====================================================
    // GET ALL INVENTORY
    // =====================================================

    @Transactional(readOnly = true)
    public List<InventoryResponse_Dto> getInventory(
            User_entity user
    ) {

        Shop_entity shop =
                getUserShop(user);


        return productRepository
                .findByShopId(shop.getId())
                .stream()
                .map(this::mapInventoryResponse)
                .toList();
    }


    // =====================================================
    // GET PRODUCT INVENTORY
    // =====================================================

    @Transactional(readOnly = true)
    public InventoryResponse_Dto getProductInventory(
            User_entity user,
            Long productId
    ) {

        Shop_entity shop =
                getUserShop(user);


        Product_entity product =
                productRepository
                        .findByIdAndShopId(
                                productId,
                                shop.getId()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Product not found in this shop"
                                )
                        );


        return mapInventoryResponse(
                product
        );
    }


    // =====================================================
    // GET STOCK MOVEMENTS
    // =====================================================

    @Transactional(readOnly = true)
    public List<StockMovementResponse_Dto>
    getStockMovements(
            User_entity user
    ) {

        Shop_entity shop =
                getUserShop(user);


        return inventoryRepository
                .findByShopIdOrderByCreatedAtDesc(
                        shop.getId()
                )
                .stream()
                .map(this::mapMovementResponse)
                .toList();
    }


    // =====================================================
    // GET PRODUCT STOCK MOVEMENTS
    // =====================================================

    @Transactional(readOnly = true)
    public List<StockMovementResponse_Dto>
    getProductStockMovements(
            User_entity user,
            Long productId
    ) {

        Shop_entity shop =
                getUserShop(user);


        // Verify product belongs to shop

        productRepository
                .findByIdAndShopId(
                        productId,
                        shop.getId()
                )
                .orElseThrow(() ->
                        new RuntimeException(
                                "Product not found in this shop"
                        )
                );


        return inventoryRepository
                .findByShopIdAndProductIdOrderByCreatedAtDesc(
                        shop.getId(),
                        productId
                )
                .stream()
                .map(this::mapMovementResponse)
                .toList();
    }


    // =====================================================
    // GET LOW STOCK
    // =====================================================

    @Transactional(readOnly = true)
    public List<InventoryResponse_Dto>
    getLowStockProducts(
            User_entity user
    ) {

        Shop_entity shop =
                getUserShop(user);


        return productRepository
                .findByShopId(shop.getId())
                .stream()
                .filter(product ->
                        product.getStockQuantity() != null
                                &&
                                product.getStockQuantity() <=
                                        product.getLowStockThreshold()
                )
                .map(this::mapInventoryResponse)
                .toList();
    }


    // =====================================================
    // GET USER SHOP
    // =====================================================

    private Shop_entity getUserShop(
            User_entity user
    ) {

        if (user == null) {

            throw new RuntimeException(
                    "User not authenticated"
            );
        }


        if (shopContextResolver.resolveActiveShop(user) == null) {

            throw new RuntimeException(
                    "User is not assigned to a shop"
            );
        }


        if (shopContextResolver.resolveActiveShop(user).getId() == null) {

            throw new RuntimeException(
                    "Shop ID is missing"
            );
        }


        return shopContextResolver.resolveActiveShop(user);
    }


    // =====================================================
    // INVENTORY MAPPER
    // =====================================================

    private InventoryResponse_Dto mapInventoryResponse(
            Product_entity product
    ) {

        Double stock =
                product.getStockQuantity();


        if (stock == null) {

            stock = 0.0;
        }


        boolean lowStock =
                stock <=
                        product.getLowStockThreshold();


        return InventoryResponse_Dto.builder()
                .productId(product.getId())
                .productName(product.getName())
                .sku(product.getSku())
                .stockQuantity(stock)
                .lowStock(lowStock)
                .build();
    }


    // =====================================================
    // MOVEMENT MAPPER
    // =====================================================

    private StockMovementResponse_Dto mapMovementResponse(
            Inventory_entity movement
    ) {

        Product_entity product =
                movement.getProduct();


        return StockMovementResponse_Dto.builder()
                .id(movement.getId())
                .productId(product.getId())
                .productName(product.getName())
                .sku(product.getSku())
                .movementType(
                        movement.getMovementType()
                )
                .quantity(
                        movement.getQuantity()
                )
                .stockBefore(
                        movement.getStockBefore()
                )
                .stockAfter(
                        movement.getStockAfter()
                )
                .reason(
                        movement.getReason()
                )
                .createdAt(
                        movement.getCreatedAt()
                )
                .build();
    }
}