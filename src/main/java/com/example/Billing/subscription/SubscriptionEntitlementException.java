package com.example.Billing.subscription;

import lombok.Getter;

@Getter
public class SubscriptionEntitlementException extends RuntimeException {

    private final String requiredTier;
    private final String errorCode;

    public SubscriptionEntitlementException(String message, String requiredTier) {
        super(message);
        this.requiredTier = requiredTier;
        this.errorCode = "PLAN_LIMIT_EXCEEDED";
    }

    public SubscriptionEntitlementException(String message, String requiredTier, String errorCode) {
        super(message);
        this.requiredTier = requiredTier;
        this.errorCode = errorCode;
    }
}
