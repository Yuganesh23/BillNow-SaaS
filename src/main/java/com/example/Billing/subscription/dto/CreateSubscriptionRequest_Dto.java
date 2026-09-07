package com.example.Billing.subscription.dto;

import com.example.Billing.subscription.SaaSPlan;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateSubscriptionRequest_Dto {

    @NotNull(message = "Plan is required")
    private SaaSPlan plan;
}