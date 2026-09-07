package com.example.Billing.product;

import com.example.Billing.auth.User_entity;
import com.example.Billing.shop.Shop_entity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private final ProductRepository productRepository;


    // =====================================================
    // CREATE PRODUCT
    // =====================================================
    public ProductResponse_Dto createProduct(
            User_entity user,
            ProductRequest_Dto request
    ) {

        Shop_entity shop = getUserShop(user);

        Long shopId = shop.getId();

        if (productRepository.existsByShopIdAndSku(
                shopId,
                request.getSku()
        )) {

            throw new RuntimeException(
                    "Product with SKU already exists in this shop"
            );
        }

        Product_entity product =
                Product_entity.builder()
                        .shop(shop)
                        .name(request.getName())
                        .sku(request.getSku())
                        .category(request.getCategory())
                        .description(request.getDescription())
                        .purchasePrice(request.getPurchasePrice())
                        .sellingPrice(request.getSellingPrice())
                        .stockQuantity(request.getStockQuantity())
                        .lowStockThreshold(
                                request.getLowStockThreshold() != null
                                        ? request.getLowStockThreshold()
                                        : 10
                        )
                        .attributes(request.getAttributes())
                        .active(true)
                        .build();

        Product_entity savedProduct =
                productRepository.save(product);

        return mapToResponse(savedProduct);
    }


    // =====================================================
    // GET ALL PRODUCTS
    // =====================================================
    @Transactional(readOnly = true)
    public List<ProductResponse_Dto> getAllProducts(
            User_entity user
    ) {

        Shop_entity shop = getUserShop(user);

        Long shopId = shop.getId();

        System.out.println(
                "Fetching products for shop ID: " + shopId
        );

        return productRepository
                .findByShopIdAndActiveTrue(shopId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }


    // =====================================================
    // GET PRODUCT BY ID
    // =====================================================
    @Transactional(readOnly = true)
    public ProductResponse_Dto getProduct(
            User_entity user,
            Long productId
    ) {

        Shop_entity shop = getUserShop(user);

        Long shopId = shop.getId();

        Product_entity product =
                productRepository
                        .findByIdAndShopId(
                                productId,
                                shopId
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Product not found"
                                )
                        );

        return mapToResponse(product);
    }


    // =====================================================
    // UPDATE PRODUCT
    // =====================================================
    public ProductResponse_Dto updateProduct(
            User_entity user,
            Long productId,
            ProductRequest_Dto request
    ) {

        Shop_entity shop = getUserShop(user);

        Long shopId = shop.getId();

        Product_entity product =
                productRepository
                        .findByIdAndShopId(
                                productId,
                                shopId
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Product not found"
                                )
                        );


        // Check duplicate SKU
        if (!product.getSku().equals(request.getSku())
                && productRepository.existsByShopIdAndSku(
                shopId,
                request.getSku()
        )) {

            throw new RuntimeException(
                    "Product with SKU already exists in this shop"
            );
        }


        product.setName(request.getName());
        product.setSku(request.getSku());
        product.setCategory(request.getCategory());
        product.setDescription(request.getDescription());
        product.setPurchasePrice(request.getPurchasePrice());
        product.setSellingPrice(request.getSellingPrice());
        product.setStockQuantity(request.getStockQuantity());

        product.setLowStockThreshold(
                request.getLowStockThreshold() != null
                        ? request.getLowStockThreshold()
                        : product.getLowStockThreshold()
        );
        product.setAttributes(request.getAttributes());

        Product_entity updatedProduct =
                productRepository.save(product);

        return mapToResponse(updatedProduct);
    }


    // =====================================================
    // DELETE PRODUCT
    // =====================================================
    public void deleteProduct(
            User_entity user,
            Long productId
    ) {

        Shop_entity shop = getUserShop(user);

        Long shopId = shop.getId();

        Product_entity product =
                productRepository
                        .findByIdAndShopId(
                                productId,
                                shopId
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Product not found"
                                )
                        );

        // Soft delete
        product.setActive(false);

        productRepository.save(product);
    }


    // =====================================================
    // RESTORE PRODUCT
    // =====================================================
    public ProductResponse_Dto restoreProduct(
            User_entity user,
            Long productId
    ) {

        Shop_entity shop = getUserShop(user);

        Long shopId = shop.getId();

        Product_entity product =
                productRepository
                        .findByIdAndShopId(
                                productId,
                                shopId
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Product not found"
                                )
                        );

        product.setActive(true);

        return mapToResponse(
                productRepository.save(product)
        );
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

        if (user.getShop() == null) {

            throw new RuntimeException(
                    "User is not assigned to a shop"
            );
        }

        if (user.getShop().getId() == null) {

            throw new RuntimeException(
                    "Shop ID is missing"
            );
        }

        return user.getShop();
    }


    // =====================================================
    // MAPPER
    // =====================================================
    private ProductResponse_Dto mapToResponse(
            Product_entity product
    ) {

        Long shopId = null;

        if (product.getShop() != null) {
            shopId = product.getShop().getId();
        }

        return ProductResponse_Dto.builder()
                .id(product.getId())
                .shopId(shopId)
                .name(product.getName())
                .sku(product.getSku())
                .category(product.getCategory())
                .description(product.getDescription())
                .purchasePrice(product.getPurchasePrice())
                .sellingPrice(product.getSellingPrice())
                .stockQuantity(product.getStockQuantity())
                .lowStockThreshold(product.getLowStockThreshold())
                .attributes(product.getAttributes())
                .active(product.isActive())
                .build();
    }
}