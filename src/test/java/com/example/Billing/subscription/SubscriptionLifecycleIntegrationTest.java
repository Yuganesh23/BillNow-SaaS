package com.example.Billing.subscription;

import com.example.Billing.auth.JwtService;
import com.example.Billing.auth.User_entity;
import com.example.Billing.auth.userRepository;
import com.example.Billing.config.BranchContextHolder;
import com.example.Billing.customer.CustomerRepository;
import com.example.Billing.product.ProductRepository;
import com.example.Billing.shop.ShopRepository;
import com.example.Billing.shop.Shop_entity;
import com.example.Billing.shop.SubscriptionTier;
import com.example.Billing.subscription.dto.ChangePlanRequest_Dto;
import com.example.Billing.subscription.dto.CreateSubscriptionRequest_Dto;
import com.example.Billing.supplier.SupplierRepository;
import com.example.Billing.support.TenantTestFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Callable;

import static com.example.Billing.subscription.SubscriptionTestSupport.assertAppError;
import static com.example.Billing.subscription.SubscriptionTestSupport.epochSeconds;
import static com.example.Billing.subscription.SubscriptionTestSupport.event;
import static com.example.Billing.subscription.SubscriptionTestSupport.json;
import static com.example.Billing.subscription.SubscriptionTestSupport.sign;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SubscriptionLifecycleIntegrationTest {
    @MockitoBean SubscriptionGateway gateway;

    @Autowired MockMvc mvc;
    @Autowired SaaSSubscriptionService service;
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
    void cancellationKeepsAccessUntilThePaidPeriodEnds() {
        var tenant = fixture.createTenant("Cancel End");
        LocalDateTime paidThrough = LocalDateTime.now().plusDays(30).truncatedTo(ChronoUnit.SECONDS);
        var subscription = paid(tenant, SaaSPlan.PRO, paidThrough);

        var response = inTenant(tenant, () -> service.cancelSubscription(tenant.owner()));

        verify(gateway).cancelAtCycleEnd(subscription.getRazorpaySubscriptionId());
        assertThat(response.isActive()).isTrue();
        assertThat(response.isCancelAtPeriodEnd()).isTrue();
        Shop_entity shop = shops.findById(tenant.shop().getId()).orElseThrow();
        assertThat(shop.getSubscriptionTier()).isEqualTo(SubscriptionTier.PRO);
        assertThat(shop.getSubscriptionEndsAt()).isEqualTo(paidThrough);

        assertAppError(() -> inTenant(tenant, () -> service.cancelSubscription(tenant.owner())),
                HttpStatus.CONFLICT, "CANCELLATION_ALREADY_SCHEDULED");
        assertAppError(() -> inTenant(tenant, () -> service.changePlan(tenant.owner(), new ChangePlanRequest_Dto(SaaSPlan.ENTERPRISE))),
                HttpStatus.CONFLICT, "SUBSCRIPTION_ENDING");
        assertAppError(() -> inTenant(tenant, () -> service.createSubscription(tenant.owner(), new CreateSubscriptionRequest_Dto(SaaSPlan.BASE))),
                HttpStatus.CONFLICT, "SUBSCRIPTION_ALREADY_ACTIVE");

        String cancelled = event("subscription.cancelled", subscription);
        service.processWebhook(cancelled, sign(cancelled));

        var ended = subscriptions.findById(subscription.getId()).orElseThrow();
        assertThat(ended.isActive()).isFalse();
        assertThat(ended.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        shop = shops.findById(tenant.shop().getId()).orElseThrow();
        assertThat(shop.getSubscriptionEndsAt()).isEqualTo(paidThrough);
        assertThat(shop.isActive()).isTrue();
    }

    @Test
    void upgradeIsImmediateButTheTierChangesOnlyAfterRazorpayConfirms() {
        var tenant = fixture.createTenant("Upgrade");
        var subscription = paid(tenant, SaaSPlan.BASE, LocalDateTime.now().plusDays(200).truncatedTo(ChronoUnit.SECONDS));

        var response = inTenant(tenant, () -> service.changePlan(tenant.owner(), new ChangePlanRequest_Dto(SaaSPlan.PRO)));

        verify(gateway).changePlan(subscription.getRazorpaySubscriptionId(), "plan_test_pro", true);
        assertThat(response.getPendingPlan()).isEqualTo(SaaSPlan.PRO);
        assertThat(response.getPlan()).isEqualTo(SaaSPlan.BASE);
        assertThat(shops.findById(tenant.shop().getId()).orElseThrow().getSubscriptionTier()).isEqualTo(SubscriptionTier.BASE);
        assertAppError(() -> inTenant(tenant, () -> service.changePlan(tenant.owner(), new ChangePlanRequest_Dto(SaaSPlan.PRO))),
                HttpStatus.CONFLICT, "PLAN_CHANGE_ALREADY_SCHEDULED");

        LocalDateTime newEnd = LocalDateTime.now().plusYears(1).truncatedTo(ChronoUnit.SECONDS);
        String updated = event("subscription.updated", subscription,
                json("plan_id", "plan_test_pro", "current_end", epochSeconds(newEnd)));
        service.processWebhook(updated, sign(updated));

        var confirmed = subscriptions.findById(subscription.getId()).orElseThrow();
        assertThat(confirmed.getPlan()).isEqualTo(SaaSPlan.PRO);
        assertThat(confirmed.getPendingPlan()).isNull();
        Shop_entity shop = shops.findById(tenant.shop().getId()).orElseThrow();
        assertThat(shop.getSubscriptionTier()).isEqualTo(SubscriptionTier.PRO);
        assertThat(shop.getSubscriptionEndsAt()).isEqualTo(newEnd);
    }

    @Test
    void downgradeIsScheduledForCycleEndAndBlockedWhileUsageExceedsTheTargetPlan() {
        var tenant = fixture.createTenant("Downgrade");
        var subscription = paid(tenant, SaaSPlan.PRO, LocalDateTime.now().plusDays(90).truncatedTo(ChronoUnit.SECONDS));
        biller(tenant, "one");
        User_entity extra = biller(tenant, "two");

        assertAppError(() -> inTenant(tenant, () -> service.changePlan(tenant.owner(), new ChangePlanRequest_Dto(SaaSPlan.BASE))),
                HttpStatus.CONFLICT, "PLAN_DOWNGRADE_BLOCKED");
        verify(gateway, never()).changePlan(anyString(), anyString(), anyBoolean());

        extra.setActive(false);
        users.save(extra);
        var scheduled = inTenant(tenant, () -> service.changePlan(tenant.owner(), new ChangePlanRequest_Dto(SaaSPlan.BASE)));
        verify(gateway).changePlan(subscription.getRazorpaySubscriptionId(), "plan_test_base", false);
        assertThat(scheduled.getPendingPlan()).isEqualTo(SaaSPlan.BASE);
        assertThat(shops.findById(tenant.shop().getId()).orElseThrow().getSubscriptionTier()).isEqualTo(SubscriptionTier.PRO);

        var reverted = inTenant(tenant, () -> service.cancelPlanChange(tenant.owner()));
        verify(gateway).cancelScheduledChange(subscription.getRazorpaySubscriptionId());
        assertThat(reverted.getPendingPlan()).isNull();
        assertAppError(() -> inTenant(tenant, () -> service.cancelPlanChange(tenant.owner())),
                HttpStatus.CONFLICT, "NO_PLAN_CHANGE_SCHEDULED");
    }

    @Test
    void planChangesRequireAnActiveTierSubscription() {
        var tenant = fixture.createTenant("No Plan");
        assertAppError(() -> inTenant(tenant, () -> service.changePlan(tenant.owner(), new ChangePlanRequest_Dto(SaaSPlan.MONTHLY))),
                HttpStatus.BAD_REQUEST, "INVALID_PLAN");
        assertAppError(() -> inTenant(tenant, () -> service.changePlan(tenant.owner(), new ChangePlanRequest_Dto(SaaSPlan.PRO))),
                HttpStatus.NOT_FOUND, "SUBSCRIPTION_NOT_FOUND");
    }

    @Test
    void resubscribingStartsWithACleanPeriod() {
        var tenant = fixture.createTenant("Resubscribe");
        var subscription = paid(tenant, SaaSPlan.BASE, LocalDateTime.now().minusDays(10).truncatedTo(ChronoUnit.SECONDS));
        subscription.setActive(false);
        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setCancelAtPeriodEnd(true);
        subscriptions.save(subscription);
        when(gateway.create(any())).thenReturn("sub_resubscribed_" + subscription.getId());

        var created = inTenant(tenant, () -> service.createSubscription(tenant.owner(), new CreateSubscriptionRequest_Dto(SaaSPlan.PRO)));

        assertThat(created.getId()).isEqualTo(subscription.getId());
        assertThat(created.getRazorpaySubscriptionId()).isEqualTo("sub_resubscribed_" + subscription.getId());
        assertThat(created.getEndDate()).isNull();
        assertThat(created.isCancelAtPeriodEnd()).isFalse();
        assertThat(created.getStatus()).isEqualTo(SubscriptionStatus.AUTHENTICATION_PENDING);
    }

    @Test
    void anotherTenantCannotCancelOrChangeThisSubscription() throws Exception {
        var owner = fixture.createTenant("Sub Owner");
        var intruder = fixture.createTenant("Sub Intruder");
        var subscription = paid(owner, SaaSPlan.PRO, LocalDateTime.now().plusDays(30).truncatedTo(ChronoUnit.SECONDS));

        mvc.perform(intruder.authenticate(post("/api/subscription/cancel"))).andExpect(status().isNotFound());
        mvc.perform(intruder.authenticate(post("/api/subscription/change-plan")
                .contentType("application/json").content("{\"plan\":\"BASE\"}"))).andExpect(status().isNotFound());
        // Pointing the branch header at the other shop is rejected before any change.
        mvc.perform(post("/api/subscription/cancel")
                .header("Authorization", "Bearer " + intruder.accessToken())
                .header("X-Branch-Id", owner.shop().getId())).andExpect(status().is4xxClientError());

        var untouched = subscriptions.findById(subscription.getId()).orElseThrow();
        assertThat(untouched.isCancelAtPeriodEnd()).isFalse();
        assertThat(untouched.getPendingPlan()).isNull();
        verify(gateway, never()).cancelAtCycleEnd(anyString());
        verify(gateway, never()).changePlan(anyString(), anyString(), anyBoolean());
    }

    private SaaSSubscription_entity paid(TenantTestFixture.Tenant tenant, SaaSPlan plan, LocalDateTime paidThrough) {
        return SubscriptionTestSupport.paidSubscription(shops, subscriptions, tenant.shop().getId(), plan, paidThrough);
    }

    private User_entity biller(TenantTestFixture.Tenant tenant, String suffix) {
        return users.save(User_entity.builder()
                .name("Biller " + suffix).email(tenant.shop().getId() + "-" + suffix + "@biller.test")
                .password("encoded").role("BILLER").active(true).shop(tenant.shop()).build());
    }

    private <T> T inTenant(TenantTestFixture.Tenant tenant, Callable<T> callable) {
        BranchContextHolder.setBranchId(tenant.shop().getId());
        try { return callable.call(); } catch (RuntimeException exception) { throw exception; }
        catch (Exception exception) { throw new AssertionError(exception); } finally { BranchContextHolder.clear(); }
    }
}
