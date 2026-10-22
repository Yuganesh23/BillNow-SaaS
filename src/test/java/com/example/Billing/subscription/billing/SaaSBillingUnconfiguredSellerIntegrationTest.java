package com.example.Billing.subscription.billing;

import com.example.Billing.auth.JwtService;
import com.example.Billing.auth.userRepository;
import com.example.Billing.customer.CustomerRepository;
import com.example.Billing.product.ProductRepository;
import com.example.Billing.shop.ShopRepository;
import com.example.Billing.subscription.SubscriptionTestSupport;
import com.example.Billing.subscription.SaaSPlan;
import com.example.Billing.subscription.SaaSSubscriptionRepository;
import com.example.Billing.subscription.SaaSSubscriptionService;
import com.example.Billing.supplier.SupplierRepository;
import com.example.Billing.support.TenantTestFixture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Without seller GST details the payment is still recorded, but no tax invoice is issued. */
@SpringBootTest(properties = "billing.saas.seller.gstin=")
@AutoConfigureMockMvc
class SaaSBillingUnconfiguredSellerIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired SaaSSubscriptionService subscriptionService;
    @Autowired SaaSSubscriptionRepository subscriptions;
    @Autowired SaaSBillingChargeRepository charges;
    @Autowired userRepository users;
    @Autowired ShopRepository shops;
    @Autowired ProductRepository products;
    @Autowired CustomerRepository customers;
    @Autowired SupplierRepository suppliers;
    @Autowired JwtService jwt;

    @Test
    void paymentIsRecordedButInvoiceDownloadIsRefused() throws Exception {
        var tenant = new TenantTestFixture(users, shops, products, customers, suppliers, jwt).createTenant("No Seller");
        var subscription = SubscriptionTestSupport.paidSubscription(shops, subscriptions, tenant.shop().getId(), SaaSPlan.BASE,
                LocalDateTime.now().plusDays(1).truncatedTo(ChronoUnit.SECONDS));
        String payload = SubscriptionTestSupport.event("subscription.charged", subscription, SubscriptionTestSupport.json(),
                SubscriptionTestSupport.json("id", "pay_unconfigured_" + tenant.shop().getId(), "amount", 499900,
                        "currency", "INR", "status", "captured"));
        subscriptionService.processWebhook(payload, SubscriptionTestSupport.sign(payload));

        var recorded = charges.findByShopIdOrderByPaidAtDescIdDesc(tenant.shop().getId());
        assertThat(recorded).hasSize(1);
        assertThat(recorded.get(0).getInvoiceNumber()).isNull();
        assertThat(recorded.get(0).getTotalAmount()).isEqualByComparingTo("4999.00");

        mvc.perform(tenant.authenticate(get("/api/subscription/charges")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].invoiceAvailable").value(false));
        mvc.perform(tenant.authenticate(get("/api/subscription/charges/{id}/invoice", recorded.get(0).getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BILLING_ENTITY_NOT_CONFIGURED"));
    }
}
