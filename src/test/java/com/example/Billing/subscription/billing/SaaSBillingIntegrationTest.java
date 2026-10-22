package com.example.Billing.subscription.billing;

import com.example.Billing.auth.JwtService;
import com.example.Billing.auth.userRepository;
import com.example.Billing.customer.CustomerRepository;
import com.example.Billing.product.ProductRepository;
import com.example.Billing.shop.ShopRepository;
import com.example.Billing.shop.Shop_entity;
import com.example.Billing.subscription.SubscriptionTestSupport;
import com.example.Billing.subscription.SaaSPlan;
import com.example.Billing.subscription.SaaSSubscriptionRepository;
import com.example.Billing.subscription.SaaSSubscriptionService;
import com.example.Billing.subscription.SaaSSubscription_entity;
import com.example.Billing.supplier.SupplierRepository;
import com.example.Billing.support.TenantTestFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "billing.saas.seller.legal-name=BillNow Test Technologies Private Limited",
        "billing.saas.seller.gstin=27AAPFU0939F1ZV",
        "billing.saas.seller.address=Pune, Maharashtra",
        "billing.saas.sac-code=998314"
})
@AutoConfigureMockMvc
class SaaSBillingIntegrationTest {
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
    TenantTestFixture fixture;

    @BeforeEach void setup() { fixture = new TenantTestFixture(users, shops, products, customers, suppliers, jwt); }

    @Test
    void chargedWebhookRecordsOneIntraStateTaxInvoicePerPayment() {
        var tenant = fixture.createTenant("Charge Intra");
        var subscription = SubscriptionTestSupport.paidSubscription(shops, subscriptions, tenant.shop().getId(), SaaSPlan.BASE,
                LocalDateTime.now().plusDays(1).truncatedTo(ChronoUnit.SECONDS));
        String paymentId = "pay_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);

        charge(subscription, paymentId, 499900);
        charge(subscription, paymentId, 499900); // Razorpay retry with a new event id

        var history = charges.findByShopIdOrderByPaidAtDescIdDesc(tenant.shop().getId());
        assertThat(history).hasSize(1);
        var invoice = history.get(0);
        assertThat(invoice.getTotalAmount()).isEqualByComparingTo("4999.00");
        assertThat(invoice.getTaxableAmount()).isEqualByComparingTo("4236.44");
        assertThat(invoice.getCgstAmount()).isEqualByComparingTo("381.28");
        assertThat(invoice.getSgstAmount()).isEqualByComparingTo("381.28");
        assertThat(invoice.getIgstAmount()).isEqualByComparingTo("0.00");
        assertThat(invoice.getPlaceOfSupplyCode()).isEqualTo("27");
        assertThat(invoice.getSupplierGstin()).isEqualTo("27AAPFU0939F1ZV");
        assertThat(invoice.getRecipientName()).isEqualTo("Charge Intra Shop");
        assertThat(invoice.getInvoiceNumber()).matches("BN/\\d{4}-\\d{2}/\\d{5}");
        assertThat(invoice.getPeriodEnd()).isNotNull();
    }

    @Test
    void otherStateShopIsChargedIgstAndInvoiceNumbersAreSequential() {
        var tenant = fixture.createTenant("Charge Inter");
        Shop_entity shop = shops.findById(tenant.shop().getId()).orElseThrow();
        shop.setStateCode("29");
        shops.save(shop);
        var subscription = SubscriptionTestSupport.paidSubscription(shops, subscriptions, shop.getId(), SaaSPlan.ENTERPRISE,
                LocalDateTime.now().plusDays(1).truncatedTo(ChronoUnit.SECONDS));

        charge(subscription, "pay_inter_first_" + shop.getId(), 2499900);
        charge(subscription, "pay_inter_second_" + shop.getId(), 2499900);

        var history = charges.findByShopIdOrderByPaidAtDescIdDesc(shop.getId());
        assertThat(history).hasSize(2);
        assertThat(history).allSatisfy(charge -> {
            assertThat(charge.getIgstAmount()).isEqualByComparingTo("3813.41");
            assertThat(charge.getCgstAmount()).isEqualByComparingTo("0.00");
            assertThat(charge.getPlaceOfSupplyCode()).isEqualTo("29");
        });
        int first = sequence(history.get(1).getInvoiceNumber());
        int second = sequence(history.get(0).getInvoiceNumber());
        assertThat(second).isGreaterThan(first);
    }

    @Test
    void ownerSeesOnlyTheirOwnHistoryAndInvoices() throws Exception {
        var owner = fixture.createTenant("History Owner");
        var other = fixture.createTenant("History Other");
        var subscription = SubscriptionTestSupport.paidSubscription(shops, subscriptions, owner.shop().getId(), SaaSPlan.PRO,
                LocalDateTime.now().plusDays(1).truncatedTo(ChronoUnit.SECONDS));
        charge(subscription, "pay_history_" + owner.shop().getId(), 999900);
        Long chargeId = charges.findByShopIdOrderByPaidAtDescIdDesc(owner.shop().getId()).get(0).getId();

        mvc.perform(owner.authenticate(get("/api/subscription/charges")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].totalAmount").value(9999.00))
                .andExpect(jsonPath("$[0].invoiceAvailable").value(true));
        mvc.perform(owner.authenticate(get("/api/subscription/charges/{id}/invoice", chargeId)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("attachment; filename=BN-")));

        mvc.perform(other.authenticate(get("/api/subscription/charges")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(other.authenticate(get("/api/subscription/charges/{id}/invoice", chargeId)))
                .andExpect(status().isNotFound());
    }

    private void charge(SaaSSubscription_entity subscription, String paymentId, long amountPaise) {
        LocalDateTime start = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        String payload = SubscriptionTestSupport.event("subscription.charged", subscription,
                SubscriptionTestSupport.json("current_start", SubscriptionTestSupport.epochSeconds(start),
                        "current_end", SubscriptionTestSupport.epochSeconds(start.plusYears(1))),
                SubscriptionTestSupport.json("id", paymentId, "amount", amountPaise, "currency", "INR",
                        "status", "captured", "created_at", SubscriptionTestSupport.epochSeconds(start)));
        subscriptionService.processWebhook(payload, SubscriptionTestSupport.sign(payload));
    }

    private static int sequence(String invoiceNumber) {
        return Integer.parseInt(invoiceNumber.substring(invoiceNumber.lastIndexOf('/') + 1));
    }
}
