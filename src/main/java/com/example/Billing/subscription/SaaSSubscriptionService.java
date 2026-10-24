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
import lombok.extern.slf4j.Slf4j;

import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.example.Billing.config.ShopContextResolver;
import com.example.Billing.shop.ShopRepository;
import com.example.Billing.shop.SubscriptionTier;
import com.example.Billing.subscription.dto.CreateOrderRequest_Dto;
import com.example.Billing.subscription.dto.VerifyOrderRequest_Dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SaaSSubscriptionService {

    private final RazorpayClient razorpayClient;
    private final ShopContextResolver shopContextResolver;

    private final RazorpayProperties razorpayProperties;

    private final SaaSSubscriptionRepository subscriptionRepository;
    private final ProcessedWebhookEventRepository processedWebhookEventRepository;
    private final ShopRepository shopRepository;

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
    ) throws RazorpayException {
        processWebhook(payload, signature, null);
    }

    @Transactional
    public void processWebhook(
            String payload,
            String signature,
            String eventIdHeader
    ) throws RazorpayException {

        // -------------------------------------------------
        // 1. Mandatory webhook signature verification
        // -------------------------------------------------
        if (signature == null || signature.isBlank()) {
            throw new IllegalArgumentException("Missing Razorpay webhook signature");
        }

        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.error("Razorpay webhook secret is not configured");
            throw new IllegalStateException("Razorpay webhook secret is not configured");
        }

        // Utils.verifyWebhookSignature throws RazorpayException if invalid
        Utils.verifyWebhookSignature(
                payload,
                signature,
                webhookSecret
        );

        JSONObject event;
        try {
            event = new JSONObject(payload);
        } catch (Exception e) {
            log.error("Invalid JSON format in Razorpay webhook payload: {}", e.getMessage());
            throw new IllegalArgumentException("Malformed JSON payload");
        }

        String eventName = event.optString("event", "unknown");

        // -------------------------------------------------
        // 2. Idempotency protection
        // -------------------------------------------------
        String eventId = (eventIdHeader != null && !eventIdHeader.isBlank())
                ? eventIdHeader.trim()
                : event.optString("event_id", event.optString("id", null));

        if (eventId != null && !eventId.isBlank()) {
            if (processedWebhookEventRepository.existsByEventId(eventId)) {
                log.info("Razorpay webhook event '{}' ({}) already processed. Skipping to maintain idempotency.", eventId, eventName);
                return;
            }
        }

        // -------------------------------------------------
        // 3. Extract subscription entity
        // -------------------------------------------------
        JSONObject payloadObj = event.optJSONObject("payload");
        if (payloadObj == null || !payloadObj.has("subscription")) {
            log.info("Razorpay webhook event '{}' does not contain subscription entity. Acknowledged.", eventName);
            recordProcessedEvent(eventId, eventName);
            return;
        }

        JSONObject subscriptionEntity = payloadObj.getJSONObject("subscription").optJSONObject("entity");
        if (subscriptionEntity == null) {
            log.warn("Subscription entity is null in webhook payload for event: {}", eventName);
            recordProcessedEvent(eventId, eventName);
            return;
        }

        String razorpaySubscriptionId = subscriptionEntity.optString("id");
        if (razorpaySubscriptionId == null || razorpaySubscriptionId.isBlank()) {
            log.warn("Subscription entity missing 'id' in webhook payload for event: {}", eventName);
            recordProcessedEvent(eventId, eventName);
            return;
        }

        // -------------------------------------------------
        // 4. Find our subscription
        // -------------------------------------------------
        Optional<SaaSSubscription_entity> optional =
                subscriptionRepository.findByRazorpaySubscriptionId(razorpaySubscriptionId);

        if (optional.isEmpty()) {
            log.warn("No local subscription found for Razorpay subscription id: {}", razorpaySubscriptionId);
            recordProcessedEvent(eventId, eventName);
            return;
        }

        SaaSSubscription_entity subscription = optional.get();

        // -------------------------------------------------
        // 5. Process events
        // -------------------------------------------------
        switch (eventName) {
            case "subscription.authenticated", "subscription.activated", "subscription.charged" -> {
                subscription.setStatus(SubscriptionStatus.ACTIVE);
                subscription.setActive(true);
                updateDates(subscription, subscriptionEntity);
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
            }
            case "subscription.completed" -> {
                subscription.setStatus(SubscriptionStatus.COMPLETED);
                subscription.setActive(false);
            }
            case "subscription.expired" -> {
                subscription.setStatus(SubscriptionStatus.EXPIRED);
                subscription.setActive(false);
            }
            default -> {
                log.info("Unhandled subscription webhook event: {}", eventName);
            }
        }

        subscriptionRepository.save(subscription);

        Shop_entity shop = subscription.getShop();
        if (shop != null) {
            shop.setActive(subscription.isActive());
        }

        recordProcessedEvent(eventId, eventName);
    }

    private void recordProcessedEvent(String eventId, String eventType) {
        if (eventId != null && !eventId.isBlank()) {
            try {
                processedWebhookEventRepository.save(
                        ProcessedWebhookEvent_entity.builder()
                                .eventId(eventId)
                                .eventType(eventType)
                                .processedAt(LocalDateTime.now())
                                .build()
                );
            } catch (Exception e) {
                log.warn("Could not save processed event {}: {}", eventId, e.getMessage());
            }
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

    // =========================================================
    // CREATE UPGRADE ORDER FOR CHECKOUT
    // =========================================================

    @Transactional
    public Map<String, Object> createUpgradeOrder(
            User_entity user,
            CreateOrderRequest_Dto request
    ) {
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        Shop_entity shop = shopRepository.findById(request.getBranchId())
                .orElseThrow(() -> new RuntimeException("Branch not found"));

        if (!shop.getOwner().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized: you do not own this shop");
        }

        long amountInPaise;
        if (request.getAmount() != null && request.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            amountInPaise = request.getAmount().multiply(new BigDecimal(100)).longValue();
        } else {
            String tierUpper = request.getTier().toUpperCase();
            amountInPaise = switch (tierUpper) {
                case "BASE" -> 49900L;
                case "PRO" -> 99900L;
                case "ENTERPRISE" -> 249900L;
                default -> 49900L;
            };
        }

        try {
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "rcpt_" + shop.getId() + "_" + (System.currentTimeMillis() % 1000000));

            JSONObject notes = new JSONObject();
            notes.put("branchId", String.valueOf(shop.getId()));
            notes.put("tier", request.getTier());
            notes.put("ownerEmail", user.getEmail());
            orderRequest.put("notes", notes);

            com.razorpay.Order order = razorpayClient.orders.create(orderRequest);
            String orderId = order.get("id");

            Map<String, Object> response = new HashMap<>();
            response.put("orderId", orderId);
            response.put("amount", amountInPaise);
            response.put("currency", "INR");
            response.put("keyId", razorpayProperties.keyId());
            response.put("tier", request.getTier());
            response.put("branchId", shop.getId());
            return response;

        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay order: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create Razorpay payment order: " + e.getMessage(), e);
        }
    }

    // =========================================================
    // VERIFY UPGRADE ORDER & ACTIVATE TIER
    // =========================================================

    @Transactional
    public Shop_entity verifyUpgradeOrder(
            User_entity user,
            VerifyOrderRequest_Dto request
    ) {
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        Shop_entity shop = shopRepository.findById(request.getBranchId())
                .orElseThrow(() -> new RuntimeException("Branch not found"));

        if (!shop.getOwner().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized: you do not own this shop");
        }

        // Cryptographic payment signature verification
        try {
            String data = request.getRazorpayOrderId() + "|" + request.getRazorpayPaymentId();
            boolean valid = Utils.verifySignature(data, request.getRazorpaySignature(), razorpayKeySecret);

            if (!valid) {
                log.warn("Invalid Razorpay payment signature for order {}", request.getRazorpayOrderId());
                throw new RuntimeException("Invalid payment signature");
            }
        } catch (RazorpayException e) {
            log.error("Razorpay signature verification error: {}", e.getMessage());
            throw new RuntimeException("Invalid payment signature: " + e.getMessage(), e);
        }

        // Upgrade shop tier and grant 30 days access
        SubscriptionTier newTier = SubscriptionTier.valueOf(request.getTier().toUpperCase());
        shop.setSubscriptionTier(newTier);
        shop.setSubscriptionEndsAt(LocalDateTime.now().plusDays(30));
        shop.setActive(true);

        Shop_entity savedShop = shopRepository.save(shop);
        log.info("Successfully upgraded shop {} ({}) to {}", shop.getName(), shop.getId(), newTier);

        // Record for idempotency
        recordProcessedEvent(request.getRazorpayPaymentId(), "order.paid." + request.getTier());

        return savedShop;
    }
}