package com.example.Billing.purchase;

import com.example.Billing.auth.User_entity;
import com.example.Billing.auth.userRepository;
import com.example.Billing.common.AppException;
import com.example.Billing.config.BranchContextHolder;
import com.example.Billing.product.ProductRepository;
import com.example.Billing.product.Product_entity;
import com.example.Billing.product.TaxType;
import com.example.Billing.shop.ShopRepository;
import com.example.Billing.shop.Shop_entity;
import com.example.Billing.supplier.SupplierRepository;
import com.example.Billing.supplier.Supplier_entity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class PurchaseTenantIsolationTest {
    @Autowired PurchaseService purchaseService;
    @Autowired userRepository users;
    @Autowired ShopRepository shops;
    @Autowired SupplierRepository suppliers;
    @Autowired ProductRepository products;
    @Autowired PurchaseRepository purchases;
    @Autowired EntityManager entityManager;

    @AfterEach
    void clearContext() {
        BranchContextHolder.clear();
    }

    @Test
    void crossTenantProductReturnsNotFoundAndRollsBackAllStockChanges() {
        User_entity ownerA = owner("owner-a@example.test");
        User_entity ownerB = owner("owner-b@example.test");
        Shop_entity shopA = shop("shop-a@example.test", ownerA);
        Shop_entity shopB = shop("shop-b@example.test", ownerB);
        ownerA.setShop(shopA);
        ownerB.setShop(shopB);
        users.saveAll(List.of(ownerA, ownerB));

        Supplier_entity supplierA = suppliers.save(Supplier_entity.builder()
                .shop(shopA).businessName("Supplier A").build());
        Product_entity ownProduct = products.save(product(shopA, "OWN-1", 10d));
        Product_entity foreignProduct = products.save(product(shopB, "FOREIGN-1", 20d));

        PurchaseRequest_Dto request = new PurchaseRequest_Dto();
        request.setSupplierId(supplierA.getId());
        request.setDiscountAmount(BigDecimal.ZERO);
        request.setItems(List.of(item(ownProduct.getId()), item(foreignProduct.getId())));
        BranchContextHolder.setBranchId(shopA.getId());

        AppException error = assertThrows(AppException.class,
                () -> purchaseService.createPurchase(ownerA, request));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatus());
        assertEquals("PRODUCT_NOT_FOUND", error.getCode());
        entityManager.clear();
        assertEquals(10d, products.findByIdAndShopId(ownProduct.getId(), shopA.getId()).orElseThrow().getStockQuantity());
        assertEquals(20d, products.findByIdAndShopId(foreignProduct.getId(), shopB.getId()).orElseThrow().getStockQuantity());
        assertEquals(0, purchases.countByShopId(shopA.getId()));
    }

    private User_entity owner(String email) {
        return users.save(User_entity.builder().name(email).email(email).password("encoded")
                .role("SHOP_OWNER").active(true).build());
    }

    private Shop_entity shop(String email, User_entity owner) {
        return shops.save(Shop_entity.builder().name(email).email(email).address("Address")
                .owner(owner).active(true).build());
    }

    private Product_entity product(Shop_entity shop, String sku, double stock) {
        return Product_entity.builder().shop(shop).name(sku).sku(sku).purchasePrice(BigDecimal.TEN)
                .sellingPrice(BigDecimal.TEN).gstRate(BigDecimal.ZERO).taxType(TaxType.NONE)
                .stockQuantity(stock).active(true).build();
    }

    private PurchaseItemRequest_Dto item(Long productId) {
        PurchaseItemRequest_Dto item = new PurchaseItemRequest_Dto();
        item.setProductId(productId);
        item.setQuantity(BigDecimal.ONE);
        item.setPurchasePrice(BigDecimal.TEN);
        return item;
    }
}
