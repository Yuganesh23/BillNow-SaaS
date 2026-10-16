package com.example.Billing.onboarding;

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
class OnboardingIntegrationTest {
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
    void checklistReflectsOnlyTheAuthenticatedShop() throws Exception {
        var tenant = fixture.createTenant("Onboarding");

        mvc.perform(tenant.authenticate(get("/api/onboarding")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSteps").value(6))
                .andExpect(jsonPath("$.steps[4].key").value("product"))
                .andExpect(jsonPath("$.steps[4].complete").value(true))
                .andExpect(jsonPath("$.steps[5].complete").value(false));
    }
}
