package com.example.Billing.subscription.dto;

import com.example.Billing.subscription.SaaSPlan;
import com.example.Billing.subscription.SubscriptionStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponse_Dto {

    private Long id;

    private Long shopId;

    private SaaSPlan plan;

    /*
     * Public Razorpay Key ID
     *
     * Safe to send to frontend.
     */
    private String razorpayKeyId;

    /*
     * Razorpay subscription ID
     */
    private String razorpaySubscriptionId;

    /*
     * Razorpay customer ID
     */
    private String razorpayCustomerId;

    private SubscriptionStatus status;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private LocalDateTime nextBillingDate;

    private boolean active;
}