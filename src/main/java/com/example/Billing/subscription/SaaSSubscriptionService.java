package com.example.Billing.subscription;

import com.example.Billing.auth.User_entity;
import com.example.Billing.config.RazorpayConfig.RazorpayProperties;
import com.example.Billing.shop.Shop_entity;
import com.example.Billing.subscription.dto.CreateSubscriptionRequest_Dto;
import com.example.Billing.subscription.dto.SubscriptionResponse_Dto;
import com.example.Billing.subscription.dto.VerifySubscriptionRequest_Dto;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;

import jakarta.transaction.Transactional;

import lombok.RequiredArgsConstructor;

import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.example.Billing.config.ShopContextResolver;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SaaSSubscriptionService {

    private final RazorpayClient razorpayClient;
    private final ShopContextResolver shopContextResolver;

    private final RazorpayProperties razorpayProperties;

    private final SaaSSubscriptionRepository subscriptionRepository;

    @Value("${razorpay.key-secret}")
    private String razorpayKeySecret;

    @Value("${razorpay.webhook-secret}")
    private String webhookSecret;

    @Value("${razorpay.monthly-plan-id}")
    private String monthlyPlanId;

    @Value("${razorpay.yearly-plan-id}")
    private String yearlyPlanId;


    // =========================================================
    // CREATE SUBSCRIPTION
    // =========================================================

    @Transactional
    public SubscriptionResponse_Dto createSubscription(
            User_entity user,
            CreateSubscriptionRequest_Dto request
    ) {

        if (user == null) {
            throw new RuntimeException("User not found");
        }

        if (!"SHOP_OWNER".equals(user.getRole())) {
            throw new RuntimeException(
                    "Only shop owner can purchase SaaS subscription"
            );
        }

        Shop_entity shop = shopContextResolver.resolveActiveShop(user);

        if (shop == null) {
            throw new RuntimeException(
                    "Shop not found for this user"
            );
        }

        // -----------------------------------------------------
        // Check existing subscription
        // -----------------------------------------------------

        Optional<SaaSSubscription_entity> existing =
                subscriptionRepository.findByShopId(
                        shop.getId()
                );

        if (existing.isPresent()) {

            SaaSSubscription_entity subscription =
                    existing.get();

            if (subscription.isActive()) {

                throw new RuntimeException(
                        "Shop already has an active subscription"
                );
            }
        }

        // -----------------------------------------------------
        // Get Razorpay plan ID
        // -----------------------------------------------------

        String planId =
                getPlanId(request.getPlan());

        int totalCount =
                request.getPlan() == SaaSPlan.MONTHLY
                        ? 120
                        : 10;

        try {

            JSONObject subscriptionRequest =
                    new JSONObject();

            subscriptionRequest.put(
                    "plan_id",
                    planId
            );

            subscriptionRequest.put(
                    "total_count",
                    totalCount
            );

            subscriptionRequest.put(
                    "quantity",
                    1
            );

            subscriptionRequest.put(
                    "customer_notify",
                    true
            );

            // -------------------------------------------------
            // Razorpay notes
            // -------------------------------------------------

            JSONObject notes =
                    new JSONObject();

            notes.put(
                    "shop_id",
                    shop.getId().toString()
            );

            notes.put(
                    "owner_id",
                    user.getId().toString()
            );

            notes.put(
                    "plan",
                    request.getPlan().name()
            );

            subscriptionRequest.put(
                    "notes",
                    notes
            );

            // -------------------------------------------------
            // Create Razorpay subscription
            // -------------------------------------------------

            com.razorpay.Subscription razorpaySubscription =
                    razorpayClient
                            .subscriptions
                            .create(subscriptionRequest);

            String razorpaySubscriptionId =
                    razorpaySubscription.get("id");

            // -------------------------------------------------
            // Save local subscription
            // -------------------------------------------------

            SaaSSubscription_entity subscription =
                    existing.orElseGet(
                            SaaSSubscription_entity::new
                    );

            subscription.setShop(shop);

            subscription.setPlan(
                    request.getPlan()
            );

            subscription.setRazorpaySubscriptionId(
                    razorpaySubscriptionId
            );

            subscription.setStatus(
                    SubscriptionStatus.AUTHENTICATION_PENDING
            );

            subscription.setActive(false);

            subscriptionRepository.save(
                    subscription
            );

            return mapToResponse(subscription);

        } catch (RazorpayException e) {

            throw new RuntimeException(
                    "Unable to create Razorpay subscription: "
                            + e.getMessage(),
                    e
            );
        }
    }


    // =========================================================
    // VERIFY PAYMENT
    // =========================================================

    @Transactional
    public SubscriptionResponse_Dto verifyPayment(
            User_entity user,
            VerifySubscriptionRequest_Dto request
    ) {

        if (user == null) {
            throw new RuntimeException(
                    "User not found"
            );
        }

        if (!"SHOP_OWNER".equals(user.getRole())) {

            throw new RuntimeException(
                    "Only shop owner can verify subscription"
            );
        }

        Shop_entity shop = shopContextResolver.resolveActiveShop(user);

        if (shop == null) {

            throw new RuntimeException(
                    "Shop not found"
            );
        }

        // -----------------------------------------------------
        // Find subscription
        // -----------------------------------------------------

        SaaSSubscription_entity subscription =
                subscriptionRepository
                        .findByRazorpaySubscriptionId(
                                request.getRazorpaySubscriptionId()
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Subscription not found"
                                )
                        );

        // -----------------------------------------------------
        // Security check
        // -----------------------------------------------------

        if (!subscription
                .getShop()
                .getId()
                .equals(shop.getId())) {

            throw new RuntimeException(
                    "Subscription does not belong to your shop"
            );
        }

        try {

            // -------------------------------------------------
            // Create signature data
            // -------------------------------------------------

            String signatureData =
                    request.getRazorpayPaymentId()
                            + "|"
                            + request.getRazorpaySubscriptionId();

            // -------------------------------------------------
            // Verify Razorpay signature
            // -------------------------------------------------

            boolean valid =
                    Utils.verifySignature(
                            signatureData,
                            request.getRazorpaySignature(),
                            razorpayKeySecret
                    );

            if (!valid) {

                throw new RuntimeException(
                        "Invalid Razorpay payment signature"
                );
            }

            // -------------------------------------------------
            // Payment verified
            // -------------------------------------------------

            subscription.setRazorpayPaymentId(
                    request.getRazorpayPaymentId()
            );

            subscription.setStatus(
                    SubscriptionStatus.ACTIVE
            );

            subscription.setActive(true);

            subscription.setStartDate(
                    LocalDateTime.now()
            );

            subscriptionRepository.save(
                    subscription
            );

            // -------------------------------------------------
            // Activate shop
            // -------------------------------------------------

            shop.setActive(true);

            return mapToResponse(subscription);

        } catch (RazorpayException e) {

            throw new RuntimeException(
                    "Payment verification failed",
                    e
            );
        }
    }


    // =========================================================
    // GET CURRENT SUBSCRIPTION
    // =========================================================

    public SubscriptionResponse_Dto getCurrentSubscription(
            User_entity user
    ) {

        if (user == null) {

            throw new RuntimeException(
                    "User not found"
            );
        }

        Shop_entity shop = shopContextResolver.resolveActiveShop(user);

        if (shop == null) {

            throw new RuntimeException(
                    "Shop not found"
            );
        }

        SaaSSubscription_entity subscription =
                subscriptionRepository
                        .findByShopId(shop.getId())
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "No subscription found"
                                )
                        );

        return mapToResponse(subscription);
    }


    // =========================================================
    // CANCEL SUBSCRIPTION
    // =========================================================

    @Transactional
    public SubscriptionResponse_Dto cancelSubscription(
            User_entity user
    ) {

        if (user == null) {

            throw new RuntimeException(
                    "User not found"
            );
        }

        if (!"SHOP_OWNER".equals(user.getRole())) {

            throw new RuntimeException(
                    "Only shop owner can cancel subscription"
            );
        }

        Shop_entity shop = shopContextResolver.resolveActiveShop(user);

        if (shop == null) {

            throw new RuntimeException(
                    "Shop not found"
            );
        }

        SaaSSubscription_entity subscription =
                subscriptionRepository
                        .findByShopId(shop.getId())
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "No subscription found"
                                )
                        );

        try {

            // -------------------------------------------------
            // Razorpay cancellation
            // -------------------------------------------------

            JSONObject cancelRequest =
                    new JSONObject();

            cancelRequest.put(
                    "cancel_at_cycle_end",
                    false
            );

            razorpayClient
                    .subscriptions
                    .cancel(
                            subscription
                                    .getRazorpaySubscriptionId(),
                            cancelRequest
                    );

            // -------------------------------------------------
            // Update local subscription
            // -------------------------------------------------

            subscription.setStatus(
                    SubscriptionStatus.CANCELLED
            );

            subscription.setActive(false);

            subscriptionRepository.save(
                    subscription
            );

            // -------------------------------------------------
            // Disable SaaS shop
            // -------------------------------------------------

            shop.setActive(false);

            return mapToResponse(subscription);

        } catch (RazorpayException e) {

            throw new RuntimeException(
                    "Unable to cancel subscription: "
                            + e.getMessage(),
                    e
            );
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

        try {

            // -------------------------------------------------
            // Verify webhook signature
            // -------------------------------------------------

            Utils.verifyWebhookSignature(
                    payload,
                    signature,
                    webhookSecret
            );

            JSONObject event =
                    new JSONObject(payload);

            String eventName =
                    event.getString("event");

            // -------------------------------------------------
            // Get subscription entity
            // -------------------------------------------------

            JSONObject subscriptionEntity =
                    event
                            .getJSONObject("payload")
                            .getJSONObject("subscription")
                            .getJSONObject("entity");

            String razorpaySubscriptionId =
                    subscriptionEntity.getString("id");

            // -------------------------------------------------
            // Find our subscription
            // -------------------------------------------------

            Optional<SaaSSubscription_entity> optional =
                    subscriptionRepository
                            .findByRazorpaySubscriptionId(
                                    razorpaySubscriptionId
                            );

            if (optional.isEmpty()) {

                return;
            }

            SaaSSubscription_entity subscription =
                    optional.get();

            // -------------------------------------------------
            // Process events
            // -------------------------------------------------

            switch (eventName) {

                case "subscription.authenticated" -> {

                    subscription.setStatus(
                            SubscriptionStatus.ACTIVE
                    );

                    subscription.setActive(true);

                    updateDates(
                            subscription,
                            subscriptionEntity
                    );
                }

                case "subscription.activated" -> {

                    subscription.setStatus(
                            SubscriptionStatus.ACTIVE
                    );

                    subscription.setActive(true);

                    updateDates(
                            subscription,
                            subscriptionEntity
                    );
                }

                case "subscription.charged" -> {

                    subscription.setStatus(
                            SubscriptionStatus.ACTIVE
                    );

                    subscription.setActive(true);

                    updateDates(
                            subscription,
                            subscriptionEntity
                    );
                }

                case "subscription.pending" -> {

                    subscription.setStatus(
                            SubscriptionStatus
                                    .AUTHENTICATION_PENDING
                    );

                    subscription.setActive(false);
                }

                case "subscription.halted" -> {

                    subscription.setStatus(
                            SubscriptionStatus.HALTED
                    );

                    subscription.setActive(false);
                }

                case "subscription.cancelled" -> {

                    subscription.setStatus(
                            SubscriptionStatus.CANCELLED
                    );

                    subscription.setActive(false);
                }

                case "subscription.completed" -> {

                    subscription.setStatus(
                            SubscriptionStatus.COMPLETED
                    );

                    subscription.setActive(false);
                }

                case "subscription.expired" -> {

                    subscription.setStatus(
                            SubscriptionStatus.EXPIRED
                    );

                    subscription.setActive(false);
                }

                default -> {
                    // Ignore unrelated events
                }
            }

            // -------------------------------------------------
            // Save subscription
            // -------------------------------------------------

            subscriptionRepository.save(
                    subscription
            );

            // -------------------------------------------------
            // Update shop access
            // -------------------------------------------------

            Shop_entity shop =
                    subscription.getShop();

            shop.setActive(
                    subscription.isActive()
            );

        } catch (RazorpayException e) {

            throw new RuntimeException(
                    "Invalid Razorpay webhook signature",
                    e
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to process Razorpay webhook",
                    e
            );
        }
    }


    // =========================================================
    // UPDATE DATES
    // =========================================================

    private void updateDates(
            SaaSSubscription_entity subscription,
            JSONObject razorpaySubscription
    ) {

        // -----------------------------------------------------
        // Current start
        // -----------------------------------------------------

        if (razorpaySubscription.has(
                "current_start"
        )) {

            long timestamp =
                    razorpaySubscription.optLong(
                            "current_start",
                            0
                    );

            if (timestamp > 0) {

                subscription.setStartDate(
                        convertTimestamp(timestamp)
                );
            }
        }

        // -----------------------------------------------------
        // Current end
        // -----------------------------------------------------

        if (razorpaySubscription.has(
                "current_end"
        )) {

            long timestamp =
                    razorpaySubscription.optLong(
                            "current_end",
                            0
                    );

            if (timestamp > 0) {

                LocalDateTime endDate =
                        convertTimestamp(timestamp);

                subscription.setEndDate(
                        endDate
                );

                subscription.setNextBillingDate(
                        endDate
                );
            }
        }

        // -----------------------------------------------------
        // Razorpay customer ID
        // -----------------------------------------------------

        if (razorpaySubscription.has(
                "customer_id"
        )) {

            String customerId =
                    razorpaySubscription.optString(
                            "customer_id",
                            null
                    );

            subscription.setRazorpayCustomerId(
                    customerId
            );
        }
    }


    // =========================================================
    // GET PLAN ID
    // =========================================================

    private String getPlanId(
            SaaSPlan plan
    ) {

        if (plan == null) {

            throw new RuntimeException(
                    "Subscription plan is required"
            );
        }

        return switch (plan) {

            case MONTHLY ->
                    monthlyPlanId;

            case YEARLY ->
                    yearlyPlanId;
        };
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

                .id(
                        subscription.getId()
                )

                .shopId(
                        subscription
                                .getShop()
                                .getId()
                )

                .plan(
                        subscription.getPlan()
                )

                // Public Razorpay Key ID
                .razorpayKeyId(
                        razorpayProperties.keyId()
                )

                .razorpaySubscriptionId(
                        subscription
                                .getRazorpaySubscriptionId()
                )

                .razorpayCustomerId(
                        subscription
                                .getRazorpayCustomerId()
                )

                .status(
                        subscription.getStatus()
                )

                .startDate(
                        subscription.getStartDate()
                )

                .endDate(
                        subscription.getEndDate()
                )

                .nextBillingDate(
                        subscription
                                .getNextBillingDate()
                )

                .active(
                        subscription.isActive()
                )

                .build();
    }
}