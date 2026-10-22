package com.example.Billing.subscription;

import com.example.Billing.auth.User_entity;
import com.example.Billing.common.AppException;
import com.example.Billing.config.RazorpayConfig.RazorpayProperties;
import com.example.Billing.config.ShopContextResolver;
import com.example.Billing.shop.Shop_entity;
import com.example.Billing.shop.SubscriptionTier;
import com.example.Billing.subscription.dto.ChangePlanRequest_Dto;
import com.example.Billing.subscription.dto.CreateSubscriptionRequest_Dto;
import com.example.Billing.subscription.dto.SubscriptionResponse_Dto;
import com.example.Billing.subscription.dto.VerifySubscriptionRequest_Dto;

import com.razorpay.RazorpayException;
import com.razorpay.Utils;

import jakarta.transaction.Transactional;

import lombok.RequiredArgsConstructor;

import org.json.JSONException;
import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SaaSSubscriptionService {

    private final SubscriptionGateway gateway;
    private final ShopContextResolver shopContextResolver;

    private final RazorpayProperties razorpayProperties;

    private final SaaSSubscriptionRepository subscriptionRepository;
    private final SubscriptionWebhookEventRepository webhookEvents;
    private final PlanLimitsService planLimitsService;

    @Value("${razorpay.key-secret}")
    private String razorpayKeySecret;

    @Value("${razorpay.webhook-secret}")
    private String webhookSecret;

    @Value("${razorpay.monthly-plan-id}")
    private String monthlyPlanId;

    @Value("${razorpay.yearly-plan-id}")
    private String yearlyPlanId;

    @Value("${razorpay.base-yearly-plan-id:${razorpay.yearly-plan-id}}")
    private String baseYearlyPlanId;

    @Value("${razorpay.pro-yearly-plan-id:${razorpay.yearly-plan-id}}")
    private String proYearlyPlanId;

    @Value("${razorpay.enterprise-yearly-plan-id:${razorpay.yearly-plan-id}}")
    private String enterpriseYearlyPlanId;

    @Value("${billing.subscription.failed-payment-grace-days:0}")
    private int failedPaymentGraceDays;


    // =========================================================
    // CREATE SUBSCRIPTION
    // =========================================================

    @Transactional
    public SubscriptionResponse_Dto createSubscription(
            User_entity user,
            CreateSubscriptionRequest_Dto request
    ) {

        Shop_entity shop = ownerShop(user);

        Optional<SaaSSubscription_entity> existing =
                subscriptionRepository.findByShopId(shop.getId());

        if (existing.isPresent() && existing.get().isActive()) {
            throw new AppException(HttpStatus.CONFLICT, "SUBSCRIPTION_ALREADY_ACTIVE",
                    existing.get().isCancelAtPeriodEnd()
                            ? "Your current plan stays active until the end of its period. Subscribe again after it ends."
                            : "This shop already has an active subscription. Change its plan instead.");
        }

        String planId = getPlanId(request.getPlan());

        int totalCount = request.getPlan() == SaaSPlan.MONTHLY ? 120 : 10;

        JSONObject notes = new JSONObject()
                .put("shop_id", shop.getId().toString())
                .put("owner_id", user.getId().toString())
                .put("plan", request.getPlan().name());

        JSONObject subscriptionRequest = new JSONObject()
                .put("plan_id", planId)
                .put("total_count", totalCount)
                .put("quantity", 1)
                .put("customer_notify", true)
                .put("notes", notes);

        String razorpaySubscriptionId = gateway.create(subscriptionRequest);

        // -----------------------------------------------------
        // Save local subscription (one row per shop is reused)
        // -----------------------------------------------------

        SaaSSubscription_entity subscription =
                existing.orElseGet(SaaSSubscription_entity::new);

        subscription.setShop(shop);
        subscription.setPlan(request.getPlan());
        subscription.setRazorpaySubscriptionId(razorpaySubscriptionId);
        subscription.setRazorpayPaymentId(null);
        subscription.setStatus(SubscriptionStatus.AUTHENTICATION_PENDING);
        subscription.setActive(false);
        subscription.setCancelAtPeriodEnd(false);
        subscription.setPendingPlan(null);

        // A reused row must not carry the previous subscription's period.
        subscription.setStartDate(null);
        subscription.setEndDate(null);
        subscription.setNextBillingDate(null);

        subscriptionRepository.save(subscription);

        return mapToResponse(subscription);
    }


    // =========================================================
    // VERIFY PAYMENT
    // =========================================================

    @Transactional
    public SubscriptionResponse_Dto verifyPayment(
            User_entity user,
            VerifySubscriptionRequest_Dto request
    ) {

        Shop_entity shop = ownerShop(user);

        SaaSSubscription_entity subscription =
                subscriptionRepository
                        .findByRazorpaySubscriptionId(request.getRazorpaySubscriptionId())
                        .filter(found -> found.getShop().getId().equals(shop.getId()))
                        .orElseThrow(this::subscriptionNotFound);

        String signatureData =
                request.getRazorpayPaymentId()
                        + "|"
                        + request.getRazorpaySubscriptionId();

        boolean valid;
        try {
            valid = Utils.verifySignature(signatureData, request.getRazorpaySignature(), razorpayKeySecret);
        } catch (RazorpayException e) {
            valid = false;
        }

        if (!valid) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_PAYMENT_SIGNATURE",
                    "Payment could not be verified. Contact support with your payment ID.");
        }

        subscription.setRazorpayPaymentId(request.getRazorpayPaymentId());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setActive(true);
        subscription.setStartDate(LocalDateTime.now());

        subscriptionRepository.save(subscription);

        syncShopAccess(shop, subscription);

        return mapToResponse(subscription);
    }


    // =========================================================
    // GET CURRENT SUBSCRIPTION
    // =========================================================

    public SubscriptionResponse_Dto getCurrentSubscription(
            User_entity user
    ) {

        Shop_entity shop = currentShop(user);

        return subscriptionRepository
                .findByShopId(shop.getId())
                .map(this::mapToResponse)
                .orElseThrow(this::subscriptionNotFound);
    }


    // =========================================================
    // CANCEL SUBSCRIPTION (AT PERIOD END)
    // =========================================================

    @Transactional
    public SubscriptionResponse_Dto cancelSubscription(
            User_entity user
    ) {

        Shop_entity shop = ownerShop(user);

        SaaSSubscription_entity subscription = activeSubscription(shop);

        if (subscription.isCancelAtPeriodEnd()) {
            throw new AppException(HttpStatus.CONFLICT, "CANCELLATION_ALREADY_SCHEDULED",
                    "Cancellation is already scheduled for the end of the current period.");
        }

        // Razorpay stops renewing at the end of the paid cycle and then sends
        // subscription.cancelled; access continues until then.
        gateway.cancelAtCycleEnd(subscription.getRazorpaySubscriptionId());

        subscription.setCancelAtPeriodEnd(true);

        subscriptionRepository.save(subscription);

        return mapToResponse(subscription);
    }


    // =========================================================
    // CHANGE PLAN
    // =========================================================

    @Transactional
    public SubscriptionResponse_Dto changePlan(
            User_entity user,
            ChangePlanRequest_Dto request
    ) {

        Shop_entity shop = ownerShop(user);

        SaaSPlan target = request.getPlan();

        SubscriptionTier targetTier = tierFor(target);

        if (targetTier == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_PLAN",
                    "Choose the BASE, PRO or ENTERPRISE plan.");
        }

        SaaSSubscription_entity subscription = activeSubscription(shop);

        if (subscription.isCancelAtPeriodEnd()) {
            throw new AppException(HttpStatus.CONFLICT, "SUBSCRIPTION_ENDING",
                    "This subscription is cancelled at the end of its period. Subscribe again after it ends.");
        }

        if (subscription.getPlan() == target) {
            throw new AppException(HttpStatus.CONFLICT, "PLAN_UNCHANGED",
                    "The shop is already on the " + target + " plan.");
        }

        if (subscription.getPendingPlan() == target) {
            throw new AppException(HttpStatus.CONFLICT, "PLAN_CHANGE_ALREADY_SCHEDULED",
                    "A change to the " + target + " plan is already in progress.");
        }

        boolean upgrade = rank(target) > rank(subscription.getPlan());

        if (!upgrade) {
            assertUsageFits(shop, targetTier);
        }

        if (subscription.getPendingPlan() != null) {
            gateway.cancelScheduledChange(subscription.getRazorpaySubscriptionId());
        }

        // Upgrades apply immediately, downgrades at the end of the paid cycle.
        // The new tier is granted only when Razorpay confirms the plan by webhook.
        gateway.changePlan(subscription.getRazorpaySubscriptionId(), getPlanId(target), upgrade);

        subscription.setPendingPlan(target);

        subscriptionRepository.save(subscription);

        return mapToResponse(subscription);
    }

    @Transactional
    public SubscriptionResponse_Dto cancelPlanChange(
            User_entity user
    ) {

        Shop_entity shop = ownerShop(user);

        SaaSSubscription_entity subscription = activeSubscription(shop);

        if (subscription.getPendingPlan() == null) {
            throw new AppException(HttpStatus.CONFLICT, "NO_PLAN_CHANGE_SCHEDULED",
                    "There is no scheduled plan change to cancel.");
        }

        gateway.cancelScheduledChange(subscription.getRazorpaySubscriptionId());

        subscription.setPendingPlan(null);

        subscriptionRepository.save(subscription);

        return mapToResponse(subscription);
    }

    private void assertUsageFits(Shop_entity shop, SubscriptionTier targetTier) {
        PlanLimitsService.Usage usage = planLimitsService.usage(shop);
        PlanLimitsService.Limits limits = planLimitsService.limitsFor(targetTier);
        if (usage.activeUsers() > limits.users() || usage.branches() > limits.branches()) {
            throw new AppException(HttpStatus.CONFLICT, "PLAN_DOWNGRADE_BLOCKED",
                    "The " + targetTier + " plan allows " + limits.users() + " active users and "
                            + limits.branches() + " branches. Deactivate users or branches before downgrading.");
        }
    }


    // =========================================================
    // RAZORPAY WEBHOOK
    // =========================================================

    @Transactional
    public void processWebhook(
            String payload,
            String signature
    ) {

        verifyWebhookSignature(payload, signature);

        JSONObject event;
        String eventName;
        JSONObject subscriptionEntity;
        String razorpaySubscriptionId;

        try {
            event = new JSONObject(payload);
            eventName = event.getString("event");
            subscriptionEntity = event
                    .getJSONObject("payload")
                    .getJSONObject("subscription")
                    .getJSONObject("entity");
            razorpaySubscriptionId = subscriptionEntity.getString("id");
        } catch (JSONException e) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_WEBHOOK_PAYLOAD",
                    "Webhook payload is not a subscription event");
        }

        Optional<SaaSSubscription_entity> optional =
                subscriptionRepository.findByRazorpaySubscriptionId(razorpaySubscriptionId);

        // Events for subscriptions this system did not create (or that a shop
        // has since replaced) are acknowledged and ignored.
        if (optional.isEmpty()) {
            return;
        }

        SaaSSubscription_entity subscription = optional.get();

        String eventId = WebhookEventIdentity.resolve(event, payload);
        if (webhookEvents.existsByEventId(eventId)) return;

        switch (eventName) {

            case "subscription.authenticated",
                 "subscription.activated",
                 "subscription.charged",
                 "subscription.resumed" -> {
                subscription.setStatus(SubscriptionStatus.ACTIVE);
                subscription.setActive(true);
                updateDates(subscription, subscriptionEntity);
                updatePlan(subscription, subscriptionEntity);
            }

            case "subscription.updated" -> {
                updateDates(subscription, subscriptionEntity);
                updatePlan(subscription, subscriptionEntity);
            }

            case "subscription.pending" -> {
                subscription.setStatus(SubscriptionStatus.AUTHENTICATION_PENDING);
                subscription.setActive(false);
            }

            case "subscription.halted" -> {
                subscription.setStatus(SubscriptionStatus.HALTED);
                subscription.setActive(false);
            }

            case "subscription.cancelled" -> {
                subscription.setStatus(SubscriptionStatus.CANCELLED);
                subscription.setActive(false);
                subscription.setPendingPlan(null);
            }

            case "subscription.completed" -> {
                subscription.setStatus(SubscriptionStatus.COMPLETED);
                subscription.setActive(false);
                subscription.setPendingPlan(null);
            }

            case "subscription.expired" -> {
                subscription.setStatus(SubscriptionStatus.EXPIRED);
                subscription.setActive(false);
                subscription.setPendingPlan(null);
            }

            default -> {
                // Ignore unrelated events
            }
        }

        subscriptionRepository.save(subscription);

        Shop_entity shop = subscription.getShop();

        syncShopAccess(shop, subscription);

        webhookEvents.save(SubscriptionWebhookEvent.builder()
                .shop(shop)
                .subscription(subscription)
                .eventId(eventId)
                .eventType(eventName)
                .payloadHash(WebhookEventIdentity.hash(payload))
                .build());
    }

    private void verifyWebhookSignature(String payload, String signature) {
        boolean valid;
        try {
            valid = Utils.verifyWebhookSignature(payload, signature, webhookSecret);
        } catch (RazorpayException e) {
            valid = false;
        }
        if (!valid) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_WEBHOOK_SIGNATURE",
                    "Invalid Razorpay webhook signature");
        }
    }


    // =========================================================
    // UPDATE DATES / PLAN FROM RAZORPAY
    // =========================================================

    private void updateDates(
            SaaSSubscription_entity subscription,
            JSONObject razorpaySubscription
    ) {

        long start = razorpaySubscription.optLong("current_start", 0);

        if (start > 0) {
            subscription.setStartDate(convertTimestamp(start));
        }

        long end = razorpaySubscription.optLong("current_end", 0);

        if (end > 0) {
            LocalDateTime endDate = convertTimestamp(end);
            subscription.setEndDate(endDate);
            subscription.setNextBillingDate(endDate);
        }

        String customerId = razorpaySubscription.optString("customer_id", null);

        if (customerId != null && !customerId.isBlank()) {
            subscription.setRazorpayCustomerId(customerId);
        }
    }

    private void updatePlan(
            SaaSSubscription_entity subscription,
            JSONObject razorpaySubscription
    ) {

        String razorpayPlanId = razorpaySubscription.optString("plan_id", null);

        if (razorpayPlanId == null || razorpayPlanId.isBlank()) return;

        planFor(razorpayPlanId).ifPresent(plan -> {
            subscription.setPlan(plan);
            if (plan == subscription.getPendingPlan()) {
                subscription.setPendingPlan(null);
            }
        });
    }


    // =========================================================
    // PLAN IDS
    // =========================================================

    private String getPlanId(
            SaaSPlan plan
    ) {

        if (plan == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_PLAN", "Subscription plan is required");
        }

        return switch (plan) {
            case MONTHLY -> monthlyPlanId;
            case YEARLY -> yearlyPlanId;
            case BASE -> baseYearlyPlanId;
            case PRO -> proYearlyPlanId;
            case ENTERPRISE -> enterpriseYearlyPlanId;
        };
    }

    /*
     * Tier plans are checked first because their ids may fall back to the
     * legacy yearly plan id when not configured separately.
     */
    Optional<SaaSPlan> planFor(String razorpayPlanId) {
        Map<SaaSPlan, String> ids = new LinkedHashMap<>();
        ids.put(SaaSPlan.BASE, baseYearlyPlanId);
        ids.put(SaaSPlan.PRO, proYearlyPlanId);
        ids.put(SaaSPlan.ENTERPRISE, enterpriseYearlyPlanId);
        ids.put(SaaSPlan.YEARLY, yearlyPlanId);
        ids.put(SaaSPlan.MONTHLY, monthlyPlanId);
        return ids.entrySet().stream()
                .filter(entry -> razorpayPlanId.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    private static SubscriptionTier tierFor(SaaSPlan plan) {
        return switch (plan) {
            case BASE -> SubscriptionTier.BASE;
            case PRO -> SubscriptionTier.PRO;
            case ENTERPRISE -> SubscriptionTier.ENTERPRISE;
            default -> null;
        };
    }

    private static int rank(SaaSPlan plan) {
        return switch (plan) {
            case BASE -> 1;
            case PRO -> 2;
            case ENTERPRISE -> 3;
            default -> 0;
        };
    }


    // =========================================================
    // SHOP ACCESS
    // =========================================================

    /*
     * Plan access is decided only by the shop's tier and subscriptionEndsAt,
     * which PlanLimitsService enforces. The shop's own active flag is never
     * used for billing, so a lapsed shop keeps read access to its data.
     */
    private void syncShopAccess(Shop_entity shop, SaaSSubscription_entity subscription) {
        if (subscription.isActive()) {
            SubscriptionTier tier = tierFor(subscription.getPlan());
            if (tier != null) shop.setSubscriptionTier(tier);
            shop.setSubscriptionEndsAt(subscription.getEndDate() != null
                    ? subscription.getEndDate()
                    : LocalDateTime.now().plusYears(1));
            return;
        }
        if (shop.getSubscriptionTier() == null || shop.getSubscriptionTier() == SubscriptionTier.TRIAL) return;
        if (subscription.getEndDate() == null) return;
        LocalDateTime paidThrough = subscription.getEndDate();
        if (isPaymentFailure(subscription.getStatus())) paidThrough = paidThrough.plusDays(failedPaymentGraceDays);
        shop.setSubscriptionEndsAt(paidThrough);
    }

    private static boolean isPaymentFailure(SubscriptionStatus status) {
        return status == SubscriptionStatus.AUTHENTICATION_PENDING
                || status == SubscriptionStatus.PAYMENT_FAILED
                || status == SubscriptionStatus.HALTED;
    }


    // =========================================================
    // LOOKUPS
    // =========================================================

    private Shop_entity currentShop(User_entity user) {
        if (user == null) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "User is not authenticated");
        }
        Shop_entity shop = shopContextResolver.resolveActiveShop(user);
        if (shop == null) {
            throw new AppException(HttpStatus.NOT_FOUND, "SHOP_NOT_FOUND", "Shop not found for this user");
        }
        return shop;
    }

    private Shop_entity ownerShop(User_entity user) {
        Shop_entity shop = currentShop(user);
        if (!"SHOP_OWNER".equals(user.getRole())) {
            throw new AppException(HttpStatus.FORBIDDEN, "OWNER_REQUIRED",
                    "Only the shop owner can manage the subscription");
        }
        return shop;
    }

    private SaaSSubscription_entity activeSubscription(Shop_entity shop) {
        SaaSSubscription_entity subscription = subscriptionRepository
                .findByShopId(shop.getId())
                .orElseThrow(this::subscriptionNotFound);
        if (!subscription.isActive()) {
            throw new AppException(HttpStatus.CONFLICT, "NO_ACTIVE_SUBSCRIPTION",
                    "This shop has no active subscription");
        }
        return subscription;
    }

    private AppException subscriptionNotFound() {
        return new AppException(HttpStatus.NOT_FOUND, "SUBSCRIPTION_NOT_FOUND", "No subscription found");
    }


    // =========================================================
    // CONVERT TIMESTAMP
    // =========================================================

    private LocalDateTime convertTimestamp(
            long timestamp
    ) {

        return LocalDateTime.ofInstant(
                Instant.ofEpochSecond(timestamp),
                ZoneId.systemDefault()
        );
    }


    // =========================================================
    // MAP RESPONSE
    // =========================================================

    private SubscriptionResponse_Dto mapToResponse(
            SaaSSubscription_entity subscription
    ) {

        return SubscriptionResponse_Dto.builder()
                .id(subscription.getId())
                .shopId(subscription.getShop().getId())
                .plan(subscription.getPlan())
                // Public Razorpay Key ID
                .razorpayKeyId(razorpayProperties.keyId())
                .razorpaySubscriptionId(subscription.getRazorpaySubscriptionId())
                .razorpayCustomerId(subscription.getRazorpayCustomerId())
                .status(subscription.getStatus())
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .nextBillingDate(subscription.getNextBillingDate())
                .active(subscription.isActive())
                .cancelAtPeriodEnd(subscription.isCancelAtPeriodEnd())
                .pendingPlan(subscription.getPendingPlan())
                .build();
    }
}
