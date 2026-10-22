package com.example.Billing.subscription;

import com.example.Billing.auth.JwtService;
import com.example.Billing.auth.userRepository;
import com.example.Billing.common.AppException;
import com.example.Billing.config.BranchContextHolder;
import com.example.Billing.customer.CustomerRepository;
import com.example.Billing.customer.CustomerService;
import com.example.Billing.invoice.CreateInvoiceRequest_Dto;
import com.example.Billing.invoice.InvoiceItemRequest_Dto;
import com.example.Billing.invoice.InvoiceService;
import com.example.Billing.product.ProductRepository;
import com.example.Billing.shop.ShopRepository;
import com.example.Billing.shop.Shop_entity;
import com.example.Billing.shop.SubscriptionTier;
import com.example.Billing.supplier.SupplierRepository;
import com.example.Billing.support.TenantTestFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "billing.subscription.failed-payment-grace-days=3")
class SubscriptionAccessIntegrationTest {
    private static final String WEBHOOK_SECRET = "test-only-webhook-secret";

    @Autowired InvoiceService invoiceService;
    @Autowired CustomerService customerService;
    @Autowired SaaSSubscriptionService subscriptionService;
    @Autowired SaaSSubscriptionRepository subscriptions;
    @Autowired userRepository users;
    @Autowired ShopRepository shops;
    @Autowired ProductRepository products;
    @Autowired CustomerRepository customers;
    @Autowired SupplierRepository suppliers;
    @Autowired JwtService jwt;
    TenantTestFixture fixture;

    @BeforeEach void setup() { fixture = new TenantTestFixture(users, shops, products, customers, suppliers, jwt); }

    @Test
    void lapsedTrialKeepsReadOnlyInvoiceAccess() {
        var tenant = fixture.createTenant("Lapsed Trial");
        var invoice = inTenant(tenant, () -> invoiceService.createInvoice(tenant.owner(), request(tenant), null));
        Shop_entity shop = shops.findById(tenant.shop().getId()).orElseThrow();
        shop.setTrialEndsAt(LocalDateTime.now().minusDays(1));
        shops.save(shop);

        assertThat(inTenant(tenant, () -> invoiceService.getAllInvoices(tenant.owner()))).hasSize(1);
        assertThat(inTenant(tenant, () -> invoiceService.getInvoice(tenant.owner(), invoice.getId())).getId())
                .isEqualTo(invoice.getId());
        assertPaymentRequired(() -> inTenant(tenant, () -> invoiceService.createInvoice(tenant.owner(), request(tenant), null)),
                "TRIAL_EXPIRED");
        assertPaymentRequired(() -> inTenant(tenant, () -> invoiceService.cancelInvoice(tenant.owner(), invoice.getId())),
                "TRIAL_EXPIRED");
    }

    @Test
    void failedRenewalEndsPaidAccessAfterGraceWithoutDisablingShop() throws Exception {
        var tenant = fixture.createTenant("Halted Renewal");
        LocalDateTime paidThrough = LocalDateTime.now().minusDays(5).truncatedTo(ChronoUnit.SECONDS);
        SaaSSubscription_entity subscription = paidSubscription(tenant.shop().getId(), SaaSPlan.PRO, paidThrough);

        subscriptionService.processWebhook(event("subscription.halted", subscription), sign(event("subscription.halted", subscription)));

        Shop_entity shop = shops.findById(tenant.shop().getId()).orElseThrow();
        assertThat(shop.isActive()).isTrue();
        assertThat(shop.getSubscriptionTier()).isEqualTo(SubscriptionTier.PRO);
        assertThat(shop.getSubscriptionEndsAt()).isEqualTo(paidThrough.plusDays(3));
        assertThat(subscriptions.findById(subscription.getId()).orElseThrow().getStatus()).isEqualTo(SubscriptionStatus.HALTED);
        assertThat(inTenant(tenant, () -> customerService.getAllCustomers(tenant.owner()))).hasSize(1);
        assertPaymentRequired(() -> inTenant(tenant, () -> invoiceService.createInvoice(tenant.owner(), request(tenant), null)),
                "SUBSCRIPTION_EXPIRED");
    }

    @Test
    void failedRenewalInsideGracePeriodStillAllowsInvoicing() throws Exception {
        var tenant = fixture.createTenant("Grace Renewal");
        LocalDateTime paidThrough = LocalDateTime.now().minusDays(1).truncatedTo(ChronoUnit.SECONDS);
        SaaSSubscription_entity subscription = paidSubscription(tenant.shop().getId(), SaaSPlan.BASE, paidThrough);

        String payload = event("subscription.pending", subscription);
        subscriptionService.processWebhook(payload, sign(payload));

        assertThat(shops.findById(tenant.shop().getId()).orElseThrow().getSubscriptionEndsAt())
                .isEqualTo(paidThrough.plusDays(3));
        assertThat(inTenant(tenant, () -> invoiceService.createInvoice(tenant.owner(), request(tenant), null)).getId())
                .isNotNull();
    }

    @Test
    void webhookWithForgedSignatureIsRejectedWithoutChangingState() {
        var tenant = fixture.createTenant("Forged Webhook");
        SaaSSubscription_entity subscription = paidSubscription(tenant.shop().getId(), SaaSPlan.BASE,
                LocalDateTime.now().plusDays(30).truncatedTo(ChronoUnit.SECONDS));
        subscription.setActive(false);
        subscription.setStatus(SubscriptionStatus.AUTHENTICATION_PENDING);
        subscriptions.save(subscription);

        String payload = event("subscription.activated", subscription);
        assertThatThrownBy(() -> subscriptionService.processWebhook(payload, "0".repeat(64)))
                .isInstanceOfSatisfying(AppException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getCode()).isEqualTo("INVALID_WEBHOOK_SIGNATURE");
                });

        SaaSSubscription_entity unchanged = subscriptions.findById(subscription.getId()).orElseThrow();
        assertThat(unchanged.isActive()).isFalse();
        assertThat(unchanged.getStatus()).isEqualTo(SubscriptionStatus.AUTHENTICATION_PENDING);
    }

    SaaSSubscription_entity paidSubscription(Long shopId, SaaSPlan plan, LocalDateTime paidThrough) {
        Shop_entity shop = shops.findById(shopId).orElseThrow();
        shop.setSubscriptionTier(SubscriptionTier.valueOf(plan.name()));
        shop.setSubscriptionEndsAt(paidThrough);
        shops.save(shop);
        return subscriptions.save(SaaSSubscription_entity.builder()
                .shop(shop).plan(plan).razorpaySubscriptionId("sub_" + UUID.randomUUID().toString().replace("-", ""))
                .status(SubscriptionStatus.ACTIVE).active(true)
                .startDate(paidThrough.minusYears(1)).endDate(paidThrough).nextBillingDate(paidThrough)
                .build());
    }

    static String event(String name, SaaSSubscription_entity subscription) {
        return """
                {"id":"evt_%s","event":"%s","payload":{"subscription":{"entity":{"id":"%s"}}}}
                """.formatted(name.replace('.', '_') + "_" + subscription.getId(), name, subscription.getRazorpaySubscriptionId()).trim();
    }

    static String sign(String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }

    static void assertPaymentRequired(Runnable call, String code) {
        assertThatThrownBy(call::run).isInstanceOfSatisfying(AppException.class, exception -> {
            assertThat(exception.getStatus()).isEqualTo(HttpStatus.PAYMENT_REQUIRED);
            assertThat(exception.getCode()).isEqualTo(code);
        });
    }

    private CreateInvoiceRequest_Dto request(TenantTestFixture.Tenant tenant) {
        InvoiceItemRequest_Dto item = new InvoiceItemRequest_Dto(); item.setProductId(tenant.product().getId()); item.setQuantity(1.0);
        CreateInvoiceRequest_Dto request = new CreateInvoiceRequest_Dto(); request.setCustomerId(tenant.customer().getId());
        request.setItems(List.of(item)); request.setDiscountAmount(BigDecimal.ZERO); request.setIsInterState(false); return request;
    }

    private <T> T inTenant(TenantTestFixture.Tenant tenant, Callable<T> callable) {
        BranchContextHolder.setBranchId(tenant.shop().getId());
        try { return callable.call(); } catch (RuntimeException exception) { throw exception; }
        catch (Exception exception) { throw new AssertionError(exception); } finally { BranchContextHolder.clear(); }
    }
}
