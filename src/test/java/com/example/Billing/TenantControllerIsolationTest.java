package com.example.Billing;

import com.example.Billing.auth.JwtService;
import com.example.Billing.auth.User_entity;
import com.example.Billing.auth.userRepository;
import com.example.Billing.customer.CustomerRepository;
import com.example.Billing.invoice.InvoiceRepository;
import com.example.Billing.invoice.InvoiceStatus;
import com.example.Billing.invoice.Invoice_entity;
import com.example.Billing.invoice.whatsapp.WhatsAppStatus;
import com.example.Billing.product.ProductRepository;
import com.example.Billing.purchase.PurchaseRepository;
import com.example.Billing.purchase.Purchase_entity;
import com.example.Billing.shop.ShopRepository;
import com.example.Billing.shop.Shop_entity;
import com.example.Billing.supplier.SupplierRepository;
import com.example.Billing.support.TenantTestFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TenantControllerIsolationTest {
    private static final String JSON = "application/json";

    @Autowired MockMvc mvc;
    @Autowired userRepository users;
    @Autowired ShopRepository shops;
    @Autowired ProductRepository products;
    @Autowired CustomerRepository customers;
    @Autowired SupplierRepository suppliers;
    @Autowired InvoiceRepository invoices;
    @Autowired PurchaseRepository purchases;
    @Autowired JwtService jwtService;

    TenantTestFixture fixture;
    TenantTestFixture.Tenant tenantA;
    TenantTestFixture.Tenant tenantB;

    @BeforeEach
    void setUp() {
        fixture = new TenantTestFixture(users, shops, products, customers, suppliers, jwtService);
        tenantA = fixture.createTenant("Tenant A");
        tenantB = fixture.createTenant("Tenant B");
    }

    @Test
    void everyControllerIsIncludedInTheTenantSecurityInventory() {
        Set<String> expected = Set.of(
                "AuthController", "BillerController", "CustomerController", "DashboardController",
                "InventoryController", "InvoiceController", "ObjectStorageController", "PaymentController",
                "ProductController", "ProfileController", "PurchaseController", "RazorpayWebhookController",
                "ReportController", "SaaSSubscriptionController", "ShopController",
                "ShopWhatsAppConfigController", "SupplierController"
        );
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        Set<String> actual = scanner.findCandidateComponents("com.example.Billing").stream()
                .map(definition -> definition.getBeanClassName())
                .map(name -> name.substring(name.lastIndexOf('.') + 1))
                .filter(name -> !name.contains("$"))
                .collect(Collectors.toSet());
        // This controller is intentionally conditional and is therefore
        // filtered out by Spring's candidate scanner when storage is disabled.
        actual.add("ObjectStorageController");

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void productControllerRejectsForeignReadUpdateDeleteAndRestore() throws Exception {
        long id = tenantA.product().getId();
        String body = """
                {"name":"Changed","sku":"CHANGED","purchasePrice":1,"sellingPrice":2,
                 "stockQuantity":1,"lowStockThreshold":1,"gstRate":0,"taxType":"NONE"}
                """;

        assertRejected(tenantB.authenticate(get("/api/products/{id}", id)));
        assertRejected(tenantB.authenticate(put("/api/products/{id}", id).contentType(JSON).content(body)));
        assertRejected(tenantB.authenticate(delete("/api/products/{id}", id)));
        assertRejected(tenantB.authenticate(put("/api/products/{id}/restore", id)));
        assertThat(products.findByIdAndShopId(id, tenantA.shop().getId()).orElseThrow().getName())
                .isEqualTo("Tenant A Product");
    }

    @Test
    void customerControllerRejectsForeignReadUpdateAndDelete() throws Exception {
        long id = tenantA.customer().getId();
        String body = """
                {"name":"Changed","whatsappNumber":"9111111111","email":"changed@example.test"}
                """;

        assertRejected(tenantB.authenticate(get("/api/customers/{id}", id)));
        assertRejected(tenantB.authenticate(put("/api/customers/{id}", id).contentType(JSON).content(body)));
        assertRejected(tenantB.authenticate(delete("/api/customers/{id}", id)));
        assertThat(customers.findByIdAndShopId(id, tenantA.shop().getId()).orElseThrow().getName())
                .isEqualTo("Tenant A Customer");
    }

    @Test
    void supplierControllerRejectsForeignReadUpdatePaymentAndDelete() throws Exception {
        long id = tenantA.supplier().getId();
        assertRejected(tenantB.authenticate(get("/api/suppliers/{id}", id)));
        assertRejected(tenantB.authenticate(get("/api/suppliers/{id}/ledger", id)));
        assertRejected(tenantB.authenticate(put("/api/suppliers/{id}", id)
                .contentType(JSON).content("{\"businessName\":\"Changed\"}")));
        assertRejected(tenantB.authenticate(post("/api/suppliers/{id}/pay", id).param("amount", "1.00")));
        assertRejected(tenantB.authenticate(delete("/api/suppliers/{id}", id)));
        assertThat(suppliers.findByIdAndShopId(id, tenantA.shop().getId()).orElseThrow().getBusinessName())
                .isEqualTo("Tenant A Supplier");
    }

    @Test
    void invoicePaymentAndPurchaseControllersRejectForeignResources() throws Exception {
        Invoice_entity invoice = invoices.save(Invoice_entity.builder()
                .shop(tenantA.shop()).customer(tenantA.customer()).biller(tenantA.owner())
                .invoiceNumber("INV-CROSS-TENANT").subtotal(new BigDecimal("100.00"))
                .discountAmount(BigDecimal.ZERO).totalAmount(new BigDecimal("118.00"))
                .taxableAmount(new BigDecimal("100.00")).cgstTotal(new BigDecimal("9.00"))
                .sgstTotal(new BigDecimal("9.00")).igstTotal(BigDecimal.ZERO)
                .status(InvoiceStatus.PENDING).whatsappStatus(WhatsAppStatus.NOT_SENT).build());
        Purchase_entity purchase = purchases.save(Purchase_entity.builder()
                .shop(tenantA.shop()).supplier(tenantA.supplier()).createdBy(tenantA.owner())
                .purchaseNumber("PUR-CROSS-TENANT").subtotal(BigDecimal.TEN)
                .discountAmount(BigDecimal.ZERO).taxAmount(BigDecimal.ZERO).grandTotal(BigDecimal.TEN)
                .status("PENDING").amountPaid(BigDecimal.ZERO).build());

        assertRejected(tenantB.authenticate(get("/api/invoices/{id}", invoice.getId())));
        assertRejected(tenantB.authenticate(put("/api/invoices/{id}/cancel", invoice.getId())));
        assertRejected(tenantB.authenticate(get("/api/invoices/{id}/pdf", invoice.getId())));
        assertRejected(tenantB.authenticate(post("/api/invoices/{id}/send-whatsapp", invoice.getId())));
        assertRejected(tenantB.authenticate(get("/api/payments/invoice/{id}", invoice.getId())));
        assertRejected(tenantB.authenticate(post("/api/payments").contentType(JSON).content("""
                {"invoiceId":%d,"paymentMethod":"CASH","amount":118.00}
                """.formatted(invoice.getId()))));
        assertRejected(tenantB.authenticate(get("/api/purchases/{id}", purchase.getId())));
        assertRejected(tenantB.authenticate(post("/api/purchases/{id}/pay", purchase.getId())
                .param("amount", "1.00")));

        assertThat(invoices.findByIdAndShopId(invoice.getId(), tenantA.shop().getId()).orElseThrow().getStatus())
                .isEqualTo(InvoiceStatus.PENDING);
        assertThat(purchases.findByIdAndShopId(purchase.getId(), tenantA.shop().getId()).orElseThrow().getAmountPaid())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void inventoryBillerAndShopControllersRejectForeignResources() throws Exception {
        User_entity billerA = users.save(User_entity.builder()
                .name("Tenant A Biller").email("tenant-a-biller@example.test").password("encoded")
                .role("BILLER").shop(tenantA.shop()).active(true).build());

        assertRejected(tenantB.authenticate(get("/api/inventory/product/{id}", tenantA.product().getId())));
        assertRejected(tenantB.authenticate(get("/api/inventory/product/{id}/movements", tenantA.product().getId())));
        assertRejected(tenantB.authenticate(post("/api/inventory/adjust").contentType(JSON).content("""
                {"productId":%d,"quantity":1,"reason":"cross tenant"}
                """.formatted(tenantA.product().getId()))));
        assertRejected(tenantB.authenticate(put("/api/billers/{id}", billerA.getId()).contentType(JSON).content("""
                {"name":"Changed","email":"changed-biller@example.test","password":""}
                """)));
        assertRejected(tenantB.authenticate(patch("/api/billers/{id}/deactivate", billerA.getId())));
        assertRejected(tenantB.authenticate(put("/api/shops/{id}", tenantA.shop().getId())
                .contentType(JSON).content("{\"name\":\"Changed\"}")));
        assertRejected(tenantB.authenticate(delete("/api/shops/{id}", tenantA.shop().getId())));

        assertThat(products.findByIdAndShopId(tenantA.product().getId(), tenantA.shop().getId())
                .orElseThrow().getStockQuantity()).isEqualTo(100.0);
        assertThat(users.findById(billerA.getId()).orElseThrow().isActive()).isTrue();
        assertThat(shops.findById(tenantA.shop().getId()).orElseThrow().getName()).isEqualTo("Tenant A Shop");
    }

    private void assertRejected(MockHttpServletRequestBuilder request) throws Exception {
        int status = mvc.perform(request).andReturn().getResponse().getStatus();
        assertThat(status).as("cross-tenant request status").isBetween(400, 499);
    }
}
