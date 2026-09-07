package com.example.Billing.subscription.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VerifySubscriptionRequest_Dto {

    @NotBlank
    private String razorpayPaymentId;

    @NotBlank
    private String razorpaySubscriptionId;

    @NotBlank
    private String razorpaySignature;
}