package com.example.Billing.subscription;

import com.example.Billing.common.AppException;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RazorpaySubscriptionGateway implements SubscriptionGateway {

    private final RazorpayClient razorpayClient;

    @Override
    public String create(JSONObject request) {
        try {
            return razorpayClient.subscriptions.create(request).get("id");
        } catch (RazorpayException e) {
            throw providerError("create subscription", e);
        }
    }

    @Override
    public void cancelAtCycleEnd(String subscriptionId) {
        try {
            razorpayClient.subscriptions.cancel(subscriptionId, new JSONObject().put("cancel_at_cycle_end", 1));
        } catch (RazorpayException e) {
            throw providerError("cancel subscription", e);
        }
    }

    @Override
    public void changePlan(String subscriptionId, String planId, boolean immediately) {
        JSONObject request = new JSONObject()
                .put("plan_id", planId)
                .put("quantity", 1)
                .put("schedule_change_at", immediately ? "now" : "cycle_end")
                .put("customer_notify", 1);
        try {
            razorpayClient.subscriptions.update(subscriptionId, request);
        } catch (RazorpayException e) {
            throw providerError("change plan", e);
        }
    }

    @Override
    public void cancelScheduledChange(String subscriptionId) {
        try {
            razorpayClient.subscriptions.cancelPendingUpdate(subscriptionId);
        } catch (RazorpayException e) {
            throw providerError("cancel scheduled plan change", e);
        }
    }

    private AppException providerError(String action, RazorpayException e) {
        log.warn("Razorpay rejected {}: {}", action, e.getMessage());
        return new AppException(HttpStatus.BAD_GATEWAY, "PAYMENT_PROVIDER_ERROR",
                "Razorpay could not " + action + ". Please try again or contact support.");
    }
}
