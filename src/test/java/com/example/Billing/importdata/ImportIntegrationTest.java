package com.example.Billing.importdata;

import com.example.Billing.auth.JwtService;
import com.example.Billing.auth.userRepository;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ImportIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired userRepository users;
    @Autowired ShopRepository shops;
    @Autowired ProductRepository products;
    @Autowired CustomerRepository customers;
    @Autowired SupplierRepository suppliers;
    @Autowired JwtService jwtService;

    private TenantTestFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new TenantTestFixture(users, shops, products, customers, suppliers, jwtService);
    }

    @Test
    void commitIsAllOrNothingWhenAnyRowIsInvalid() throws Exception {
        var tenant = fixture.createTenant("Atomic import");
        long before = products.countByShopIdAndActiveTrue(tenant.shop().getId());
        String body = """
                {"type":"PRODUCTS","rows":[
                  {"name":"Valid","sku":"IMPORT-OK","purchase_price":"10.00","selling_price":"12.00","stock_quantity":"5"},
                  {"name":"Broken","sku":"IMPORT-BAD","purchase_price":"oops","selling_price":"12.00","stock_quantity":"5"}
                ]}
                """;

        mvc.perform(tenant.authenticate(post("/api/imports/commit")).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("IMPORT_VALIDATION_FAILED"));

        assertThat(products.countByShopIdAndActiveTrue(tenant.shop().getId())).isEqualTo(before);
        assertThat(products.existsByShopIdAndSku(tenant.shop().getId(), "IMPORT-OK")).isFalse();
    }

    @Test
    void validProductRowsAreCommittedToAuthenticatedTenantOnly() throws Exception {
        var tenantA = fixture.createTenant("Import A");
        var tenantB = fixture.createTenant("Import B");
        String body = """
                {"type":"PRODUCTS","rows":[
                  {"name":"Imported product","sku":"IMPORTED-1","purchase_price":"10.00","selling_price":"12.50","stock_quantity":"5","tax_type":"NONE"}
                ]}
                """;

        mvc.perform(tenantA.authenticate(post("/api/imports/commit")).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importedRows").value(1));

        assertThat(products.existsByShopIdAndSku(tenantA.shop().getId(), "IMPORTED-1")).isTrue();
        assertThat(products.existsByShopIdAndSku(tenantB.shop().getId(), "IMPORTED-1")).isFalse();
    }

    @Test
    void openingBalanceCannotReferenceAnotherTenantAccount() throws Exception {
        var tenantA = fixture.createTenant("Balance A");
        var tenantB = fixture.createTenant("Balance B");
        String foreignNumber = tenantB.customer().getWhatsappNumber();
        String body = """
                {"type":"OPENING_BALANCES","rows":[
                  {"account_type":"CUSTOMER","identifier":"%s","amount":"500.00"}
                ]}
                """.formatted(foreignNumber);

        mvc.perform(tenantA.authenticate(post("/api/imports/commit")).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        assertThat(customers.findByIdAndShopId(tenantB.customer().getId(), tenantB.shop().getId())
                .orElseThrow().getCreditBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
