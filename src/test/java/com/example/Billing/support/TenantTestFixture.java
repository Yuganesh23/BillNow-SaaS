package com.example.Billing.support;

import com.example.Billing.auth.JwtService;
import com.example.Billing.auth.User_entity;
import com.example.Billing.auth.userRepository;
import com.example.Billing.config.BranchContextHolder;
import com.example.Billing.customer.CustomerRepository;
import com.example.Billing.customer.Customer_entity;
import com.example.Billing.product.ProductRepository;
import com.example.Billing.product.Product_entity;
import com.example.Billing.product.TaxType;
import com.example.Billing.shop.ShopRepository;
import com.example.Billing.shop.Shop_entity;
import com.example.Billing.supplier.SupplierRepository;
import com.example.Billing.supplier.Supplier_entity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Creates complete, independent tenants for integration tests. The returned
 * request helper authenticates through the same JWT filter and branch header
 * used by production requests.
 */
public final class TenantTestFixture {
    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    private final userRepository users;
    private final ShopRepository shops;
    private final ProductRepository products;
    private final CustomerRepository customers;
    private final SupplierRepository suppliers;
    private final JwtService jwtService;

    public TenantTestFixture(
            userRepository users,
            ShopRepository shops,
            ProductRepository products,
            CustomerRepository customers,
            SupplierRepository suppliers,
            JwtService jwtService
    ) {
        this.users = users;
        this.shops = shops;
        this.products = products;
        this.customers = customers;
        this.suppliers = suppliers;
        this.jwtService = jwtService;
    }

    public Tenant createTenant(String label) {
        int suffix = SEQUENCE.incrementAndGet();
        String key = label.toLowerCase().replaceAll("[^a-z0-9]", "-") + "-" + suffix;

        User_entity owner = users.save(User_entity.builder()
                .name(label + " Owner")
                .email(key + "@example.test")
                .password("encoded-test-password")
                .role("SHOP_OWNER")
                .active(true)
                .build());

        Shop_entity shop = shops.save(Shop_entity.builder()
                .name(label + " Shop")
                .email(key + "-shop@example.test")
                .address("Test address")
                .owner(owner)
                .active(true)
                .trialEndsAt(java.time.LocalDateTime.now().plusDays(7))
                .build());
        owner.setShop(shop);
        owner = users.save(owner);

        Product_entity product = products.save(Product_entity.builder()
                .shop(shop)
                .name(label + " Product")
                .sku("SKU-" + suffix)
                .purchasePrice(new BigDecimal("50.00"))
                .sellingPrice(new BigDecimal("100.00"))
                .gstRate(new BigDecimal("18.00"))
                .taxType(TaxType.EXCLUSIVE)
                .stockQuantity(100.0)
                .lowStockThreshold(10.0)
                .active(true)
                .build());

        Customer_entity customer = customers.save(Customer_entity.builder()
                .shop(shop)
                .name(label + " Customer")
                .whatsappNumber("900000" + String.format("%04d", suffix))
                .email(key + "-customer@example.test")
                .build());

        Supplier_entity supplier = suppliers.save(Supplier_entity.builder()
                .shop(shop)
                .businessName(label + " Supplier")
                .phone("800000" + String.format("%04d", suffix))
                .build());

        BranchContextHolder.setBranchId(shop.getId());
        String accessToken;
        try {
            accessToken = jwtService.generateToken(owner);
        } finally {
            BranchContextHolder.clear();
        }
        return new Tenant(owner, shop, product, customer, supplier, accessToken);
    }

    public record Tenant(
            User_entity owner,
            Shop_entity shop,
            Product_entity product,
            Customer_entity customer,
            Supplier_entity supplier,
            String accessToken
    ) {
        public MockHttpServletRequestBuilder authenticate(MockHttpServletRequestBuilder request) {
            return request
                    .header("Authorization", "Bearer " + accessToken)
                    .header("X-Branch-Id", shop.getId());
        }

        public void authenticate(MockHttpServletRequest request) {
            request.addHeader("Authorization", "Bearer " + accessToken);
            request.addHeader("X-Branch-Id", shop.getId());
        }
    }
}
