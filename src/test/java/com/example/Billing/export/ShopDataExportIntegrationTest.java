package com.example.Billing.export;

import com.example.Billing.auth.JwtService;
import com.example.Billing.auth.User_entity;
import com.example.Billing.auth.userRepository;
import com.example.Billing.config.BranchContextHolder;
import com.example.Billing.customer.CustomerRepository;
import com.example.Billing.product.ProductRepository;
import com.example.Billing.shop.ShopRepository;
import com.example.Billing.supplier.SupplierRepository;
import com.example.Billing.support.TenantTestFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ShopDataExportIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired userRepository users;
    @Autowired ShopRepository shops;
    @Autowired ProductRepository products;
    @Autowired CustomerRepository customers;
    @Autowired SupplierRepository suppliers;
    @Autowired JwtService jwt;
    TenantTestFixture fixture;

    @BeforeEach void setup() { fixture = new TenantTestFixture(users, shops, products, customers, suppliers, jwt); }

    @Test
    void jsonExportIsTenantScopedAndNeverContainsPasswords() throws Exception {
        var tenantA = fixture.createTenant("Export A");
        fixture.createTenant("Export B");

        String json = mvc.perform(tenantA.authenticate(get("/api/data-export/shop.json")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".json")))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(json).contains("Export A Product", "Export A Customer");
        assertThat(json).doesNotContain("Export B Product", "Export B Customer", "encoded-test-password", "idempotency_key");
    }

    @Test
    void zipContainsPortableJsonAndCsvFilesForOnlyTheActiveShop() throws Exception {
        var tenantA = fixture.createTenant("Zip A");
        fixture.createTenant("Zip B");
        byte[] payload = mvc.perform(tenantA.authenticate(get("/api/data-export/shop.zip")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();

        Map<String, String> entries = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(payload), StandardCharsets.UTF_8)) {
            java.util.zip.ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) entries.put(entry.getName(), new String(zip.readAllBytes(), StandardCharsets.UTF_8));
        }
        assertThat(entries).containsKeys("billnow-export.json", "products.csv", "invoices.csv", "payments.csv",
                "inventoryMovements.csv", "quotations.csv", "recurringInvoices.csv", "eInvoiceRecords.csv");
        assertThat(entries.get("products.csv")).contains("Zip A Product").doesNotContain("Zip B Product");
    }

    @Test
    void billerCannotExportTheShop() throws Exception {
        var tenant = fixture.createTenant("Owner only");
        User_entity biller = users.save(User_entity.builder()
                .name("Export Biller")
                .email("export-biller@example.test")
                .password("encoded-test-password")
                .role("BILLER")
                .active(true)
                .shop(tenant.shop())
                .build());
        BranchContextHolder.setBranchId(tenant.shop().getId());
        String token;
        try { token = jwt.generateToken(biller); }
        finally { BranchContextHolder.clear(); }

        mvc.perform(get("/api/data-export/shop.zip")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Branch-Id", tenant.shop().getId()))
                .andExpect(status().isForbidden());
    }
}
