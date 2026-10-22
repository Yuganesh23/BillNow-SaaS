package com.example.Billing.subscription;

import com.example.Billing.common.AppException;
import com.example.Billing.shop.ShopRepository;
import com.example.Billing.shop.Shop_entity;
import com.example.Billing.shop.SubscriptionTier;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Builds paid subscriptions and signed Razorpay webhook payloads for tests. */
final class SubscriptionTestSupport {
    static final String WEBHOOK_SECRET = "test-only-webhook-secret";
    private static final AtomicInteger EVENTS = new AtomicInteger();

    private SubscriptionTestSupport() {}

    static SaaSSubscription_entity paidSubscription(ShopRepository shops, SaaSSubscriptionRepository subscriptions,
                                                    Long shopId, SaaSPlan plan, LocalDateTime paidThrough) {
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

    /** A webhook event; {@code entity} is merged into the subscription entity. */
    static String event(String name, SaaSSubscription_entity subscription, JSONObject entity) {
        return event(name, subscription, entity, null);
    }

    /** A webhook event that also carries a payment entity, as subscription.charged does. */
    static String event(String name, SaaSSubscription_entity subscription, JSONObject entity, JSONObject payment) {
        try {
            JSONObject subscriptionEntity = new JSONObject(entity.toString()).put("id", subscription.getRazorpaySubscriptionId());
            JSONObject payload = new JSONObject().put("subscription", new JSONObject().put("entity", subscriptionEntity));
            if (payment != null) payload.put("payment", new JSONObject().put("entity", payment));
            return new JSONObject()
                    .put("id", "evt_" + EVENTS.incrementAndGet() + "_" + UUID.randomUUID())
                    .put("event", name)
                    .put("payload", payload)
                    .toString();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    static JSONObject json(Object... keyValues) {
        try {
            JSONObject object = new JSONObject();
            for (int i = 0; i < keyValues.length; i += 2) object.put((String) keyValues[i], keyValues[i + 1]);
            return object;
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    static String event(String name, SaaSSubscription_entity subscription) {
        return event(name, subscription, new JSONObject());
    }

    static long epochSeconds(LocalDateTime time) {
        return time.atZone(ZoneId.systemDefault()).toEpochSecond();
    }

    static String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    static void assertAppError(Runnable call, HttpStatus status, String code) {
        assertThatThrownBy(call::run).isInstanceOfSatisfying(AppException.class, exception -> {
            assertThat(exception.getStatus()).isEqualTo(status);
            assertThat(exception.getCode()).isEqualTo(code);
        });
    }
}
