package com.example.Billing;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiDocumentationIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired userRepository users;
    @Autowired ShopRepository shops;
    @Autowired ProductRepository products;
    @Autowired CustomerRepository customers;
    @Autowired SupplierRepository suppliers;
    @Autowired JwtService jwtService;

    TenantTestFixture.Tenant tenant;

    @BeforeEach
    void setUp() {
        tenant = new TenantTestFixture(users, shops, products, customers, suppliers, jwtService)
                .createTenant("API Docs");
    }

    @Test
    void generatedContractIsAuthenticatedAndDescribesTheCookieScheme() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isForbidden());

        mvc.perform(tenant.authenticate(get("/v3/api-docs")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.info.title").value("XenBill API"))
                .andExpect(jsonPath("$.components.securitySchemes.cookieAuth.type").value("apiKey"))
                .andExpect(jsonPath("$.paths['/api/invoices']").exists())
                .andExpect(jsonPath("$.paths['/api/portal']").exists());

        mvc.perform(tenant.authenticate(get("/swagger-ui.html")))
                .andExpect(status().is3xxRedirection());
    }
}
