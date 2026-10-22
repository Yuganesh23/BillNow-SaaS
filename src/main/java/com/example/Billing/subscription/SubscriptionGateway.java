package com.example.Billing.subscription;

import org.json.JSONObject;

/**
 * Payment-provider operations used by SaaS subscription billing. Kept behind an
 * interface so subscription rules can be tested without calling Razorpay.
 */
public interface SubscriptionGateway {

    /** Creates a provider subscription and returns its id. */
    String create(JSONObject request);

    /** Schedules cancellation at the end of the current paid period. */
    void cancelAtCycleEnd(String subscriptionId);

    /** Moves the subscription to another provider plan, now or at cycle end. */
    void changePlan(String subscriptionId, String planId, boolean immediately);

    /** Drops a plan change that was scheduled for the end of the cycle. */
    void cancelScheduledChange(String subscriptionId);
}
